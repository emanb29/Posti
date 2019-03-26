package me.ethanbell.posti

import android.content.*
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.runBlocking
import java.io.File


object Util {
    /**
     * Invoke the ProcessImageActivity
     */
    fun prepImage(ctx: Context, uri: Uri) {
        val intent = Intent(ctx, ProcessImageActivity::class.java).apply {
            action = "me.ethanbell.posti.PROCESSIMG"
            data = uri
        }.setFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(ctx, intent, Bundle.EMPTY)
    }

    /**
     * Invoke Instagram's "post image" activity
     */
    fun postImage(ctx: Context, uri: Uri) {
        if (runCatching { ctx.packageManager.getPackageInfo("com.instagram.android", 0) }.isSuccess) {
            val instaPost = Intent(ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri)
                .setFlags(FLAG_GRANT_READ_URI_PERMISSION).setPackage("com.instagram.android")
            startActivity(ctx, instaPost, Bundle.EMPTY)
        } else {
            //Instagram not installed, defer to generic image intent
            val genericPost = Intent(ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri)
                .setFlags(FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(ctx, genericPost, Bundle.EMPTY)
        }
    }

    /**
     * Determine if a http(s) uri could be an image based off its extension and/or MIME type
     */
    private fun couldBeDirectImage(uri: Uri): Boolean {
        val couldBe: Boolean? = uri.lastPathSegment?.let { filename ->
            {
                // url ends with image extension
                setOf("png", "gif", "jpg", "jpeg", "bmp", "webp", "heic", "heif").any {
                    filename.endsWith(
                        it,
                        true
                    )
                }
            }()
                    ||
                    kotlin.runCatching {
                        // http MIME type is image*
                        val resp = runBlocking {
                            uri.toString().httpGet().allowRedirects(true).awaitByteArrayResponse()

                        }.second

                        val contentType: String? = resp.header(Headers.CONTENT_TYPE).firstOrNull()
                        contentType?.startsWith("image", true) ?: false
                    }.getOrDefault(false)
        }
        return (couldBe ?: false)
    }

    /**
     * Given a web Uri known to point to an image, download and return a local cached copy of that image
     */
    private fun downloadVerifiedImage(uri: Uri): File {
        lateinit var file: File
        runBlocking {
            uri.toString().httpDownload()
                .fileDestination { _, _ ->
                    File.createTempFile("postiDownload", "").apply { file = this }
                }
                .progress { _, _ -> }
                .allowRedirects(true)
                .awaitByteArrayResponse()
        }
        return file
    }

    /**
     * Given a uri known to be http or https, if it points to an image-like object, return a uri to a direct image. Otherwise, null.
     */
    fun getDirectImageUri(uri: Uri): Uri? {
        return when {
            couldBeDirectImage(uri) -> uri
            Instagram.isInstaImage(uri) ->
                Uri.parse("https://instagram.com/p/${Instagram.shortCode(uri)}/media/?size=l")
            Reddit.maybeRedditImage(uri) -> Reddit.getRedditImage(uri)
//            TODO("Match facebook URLs") -> TODO("Download facebook photo")
//            TODO("Match twitter URLs") -> TODO("Download twitter photo")
            else -> null

        }
    }

    /**
     * Given a ClipData, if the ClipData is an image or a URI referring to an image, return a Uri for a locally-cached
     * copy of the image file. Otherwise null
     */
    fun cacheImageFromClip(ctx: Context, clip: ClipData?): Uri? {
        return clip?.itemCount?.let { count ->
            val item = clip.getItemAt(0)
            val type = clip.description.getMimeType(0)!!
            when { // Generate a preliminary (nonlocal) URI
                type.contains("image/", true) -> item.uri
                type.contentEquals(ClipDescription.MIMETYPE_TEXT_URILIST) -> item.uri
                type.contentEquals(ClipDescription.MIMETYPE_TEXT_PLAIN) -> item.text.toString().runCatching {
                    Uri.parse(
                        this
                    )
                }.getOrNull()
                else -> null
            }?.normalizeScheme()?.let { uri ->
                // Convert the URI into something controlled by the application - a Uri for a temporary file
                uri.scheme?.let { scheme ->
                    when { // Generate a temp file
                        scheme.contains("http", true) -> getDirectImageUri(uri)?.let { downloadVerifiedImage(it) }
                        scheme.contentEquals(ContentResolver.SCHEME_CONTENT) ||
                                scheme.contentEquals(ContentResolver.SCHEME_FILE) -> {
                            ctx.contentResolver.openInputStream(uri)?.let { inStr ->
                                // copy local file to an application-controlled temporary file
                                File.createTempFile("postiLocalCopy", "").apply {
                                    writeBytes(inStr.readBytes())
                                    inStr.close()
                                }
                            }
                        }
                        else -> null
                    }?.let { FileProvider.getUriForFile(ctx, "me.ethanbell.posti.fileprovider", it) } // Make URI
                }
            }
        }
    }
}