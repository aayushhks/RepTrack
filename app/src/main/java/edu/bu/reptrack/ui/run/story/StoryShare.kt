package edu.bu.reptrack.ui.run.story

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

// Sharing the story card with other apps through implicit intents (Lecture 4). Other apps
// cannot read our files directly, so the picture goes out as a content:// link from the
// FileProvider in the manifest, with permission to read only that one file.

const val INSTAGRAM_PACKAGE = "com.instagram.android"

/** Where a picture can go in Instagram: straight to its story screen, or to Instagram's own picker. */
sealed interface InstagramTarget {
    data class Story(val component: ComponentName) : InstagramTarget

    data object AnyScreen : InstagramTarget
}

/** Which of Instagram's share screens is the story one. Plain Kotlin so it is easy to test. */
object InstagramTargets {
    /**
     * Instagram offers several screens for a shared picture (feed, story, chats). Their names
     * are not documented, but the story one has "Story" in it. Null if none does.
     */
    fun pickStoryScreen(activityNames: List<String>): String? =
        activityNames.firstOrNull { it.contains("story", ignoreCase = true) }
}

/** Where Instagram can take the picture, or null if Instagram is not installed. */
fun Context.findInstagram(): InstagramTarget? {
    val screens = queryActivities(sendImageIntent().setPackage(INSTAGRAM_PACKAGE))
    if (screens.isEmpty()) return null
    val story = InstagramTargets.pickStoryScreen(screens.map { it.activityInfo.name })
        ?: return InstagramTarget.AnyScreen
    return InstagramTarget.Story(ComponentName(INSTAGRAM_PACKAGE, story))
}

/**
 * Opens Instagram with the card ready to post as a story. This uses the plain share intent, so
 * it needs no Meta developer app id. Returns false if Instagram could not be opened.
 */
fun Context.shareToInstagramStory(picture: File): Boolean {
    val target = findInstagram() ?: return false
    val intent = sendImageIntent(contentUri(picture)).apply {
        when (target) {
            is InstagramTarget.Story -> component = target.component
            InstagramTarget.AnyScreen -> setPackage(INSTAGRAM_PACKAGE)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        startActivity(intent)
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    } catch (blocked: SecurityException) {
        false
    }
}

/** The system share sheet with every app that takes a picture: Messages, WhatsApp, Drive and more. */
fun Context.shareImage(picture: File, chooserTitle: String) {
    val chooser = Intent.createChooser(sendImageIntent(contentUri(picture)), chooserTitle)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
}

private fun Context.contentUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

private fun sendImageIntent(uri: Uri? = null) = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    if (uri != null) {
        putExtra(Intent.EXTRA_STREAM, uri)
        // The ClipData also carries the read permission, and gives the share sheet its preview.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun Context.queryActivities(intent: Intent): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }
