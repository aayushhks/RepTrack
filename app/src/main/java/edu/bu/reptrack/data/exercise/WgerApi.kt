package edu.bu.reptrack.data.exercise

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** wger's English language id. Translations come in every language, we only keep this one. */
const val WGER_ENGLISH = 2

/** Where the exercise catalog comes from. A fake replaces it in tests. */
interface ExerciseApi {
    /** One page of the catalog. Throws if the network or the server fails. */
    suspend fun fetchPage(offset: Int, limit: Int): WgerPageDto
}

/** Reads the public wger REST API with Ktor. It needs no key. */
class WgerApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://wger.de/api/v2",
) : ExerciseApi {

    override suspend fun fetchPage(offset: Int, limit: Int): WgerPageDto =
        client.get("$baseUrl/exerciseinfo/") {
            parameter("language", WGER_ENGLISH)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
}

/** The Ktor client for wger. Anything but a 2xx answer throws, so the screen can show the error. */
fun wgerHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) {
        // wger sends many fields we do not use.
        json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
    }
}

@Serializable
data class WgerPageDto(
    val results: List<WgerExerciseDto> = emptyList(),
    /** Url of the next page, null on the last one. */
    val next: String? = null,
)

@Serializable
data class WgerExerciseDto(
    val id: Int,
    val category: WgerNameDto? = null,
    val muscles: List<WgerMuscleDto> = emptyList(),
    val equipment: List<WgerNameDto> = emptyList(),
    val images: List<WgerImageDto> = emptyList(),
    val translations: List<WgerTranslationDto> = emptyList(),
)

@Serializable
data class WgerNameDto(val name: String? = null)

@Serializable
data class WgerMuscleDto(
    val name: String? = null,
    @SerialName("name_en") val nameEn: String? = null,
)

@Serializable
data class WgerImageDto(
    val image: String? = null,
    @SerialName("is_main") val isMain: Boolean = false,
    val thumbnails: WgerThumbnailsDto? = null,
)

@Serializable
data class WgerThumbnailsDto(val small: String? = null)

@Serializable
data class WgerTranslationDto(
    val name: String? = null,
    val description: String? = null,
    val language: Int = 0,
)
