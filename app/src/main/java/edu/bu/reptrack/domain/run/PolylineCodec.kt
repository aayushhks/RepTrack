package edu.bu.reptrack.domain.run

import kotlin.math.roundToLong

/**
 * Stores a route as short text in Google's "encoded polyline" format, which map services
 * use too. Each point is saved as the change from the one before, to five decimals (about
 * 1 m), so a 5 km run of about 1,500 points takes a few kilobytes in one row instead of
 * 1,500 rows. Plain Kotlin so it is easy to test.
 */
object PolylineCodec {
    private const val FACTOR = 1e5

    fun encode(points: List<RoutePoint>): String {
        val out = StringBuilder(points.size * 6)
        var lastLat = 0L
        var lastLng = 0L
        for (point in points) {
            val lat = (point.latitude * FACTOR).roundToLong()
            val lng = (point.longitude * FACTOR).roundToLong()
            encodeNumber(lat - lastLat, out)
            encodeNumber(lng - lastLng, out)
            lastLat = lat
            lastLng = lng
        }
        return out.toString()
    }

    /** Reads a route back. Damaged text gives the points read before the damage, never a crash. */
    fun decode(encoded: String): List<RoutePoint> {
        val points = ArrayList<RoutePoint>(encoded.length / 6)
        val reader = Reader(encoded)
        var lat = 0L
        var lng = 0L
        while (!reader.isAtEnd) {
            val dLat = reader.next() ?: break
            val dLng = reader.next() ?: break
            lat += dLat
            lng += dLng
            points += RoutePoint(lat / FACTOR, lng / FACTOR)
        }
        return points
    }

    // A number is shifted left one bit (inverted if negative), then written five bits at a
    // time, lowest first. Each chunk but the last has 0x20 set, and 63 makes it a printable character.
    private fun encodeNumber(value: Long, out: StringBuilder) {
        var rest = if (value < 0) (value shl 1).inv() else value shl 1
        while (rest >= 0x20) {
            out.append(((0x20 or (rest and 0x1f).toInt()) + 63).toChar())
            rest = rest ushr 5
        }
        out.append((rest.toInt() + 63).toChar())
    }

    private class Reader(private val text: String) {
        private var index = 0

        val isAtEnd: Boolean get() = index >= text.length

        /** The next number, or null if the text ends in the middle of one or has a stray character. */
        fun next(): Long? {
            var result = 0L
            var shift = 0
            while (true) {
                if (index >= text.length || shift > MAX_SHIFT) return null
                val chunk = text[index++].code - 63
                if (chunk !in 0..63) return null
                result = result or ((chunk and 0x1f).toLong() shl shift)
                shift += 5
                if (chunk < 0x20) break
            }
            return if ((result and 1L) != 0L) (result shr 1).inv() else result shr 1
        }
    }

    /** Longer than any real coordinate needs: stops on garbage instead of overflowing. */
    private const val MAX_SHIFT = 60
}
