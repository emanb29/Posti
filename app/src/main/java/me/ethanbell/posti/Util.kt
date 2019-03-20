package me.ethanbell.posti

import android.content.*
import android.content.Intent.ACTION_SEND
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpHead
import kotlinx.coroutines.runBlocking
import java.io.File

typealias VerifiedImageUri = Uri

object Util {
    fun prepImage(ctx: Context, uri: Uri) {
        val intent = Intent(ctx, ProcessImageActivity::class.java).apply {
            action = "me.ethanbell.posti.PROCESSIMG"
            data = uri
        }
        startActivity(ctx, intent, Bundle.EMPTY)
    }

    fun postImage(ctx: Context, uri: Uri) {
        val instaPost = Intent(ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri)
            .setFlags(FLAG_GRANT_READ_URI_PERMISSION).setPackage("com.instagram.android")
        startActivity(ctx, instaPost, Bundle.EMPTY)
    }

    private fun couldBeImage(uri: Uri): Boolean {
        val couldBe: Boolean? = uri.lastPathSegment?.let { filename ->
            {
                // url ends with image extension
                setOf("png", "gif", "jpg", "jpeg", "bmp", "webp", "heic", "heif").map {
                    filename.endsWith(
                        it,
                        true
                    )
                }.reduce { l, r -> l || r }
            }()
                    ||
                    {
                        // http MIME type is image*
                        val resp = runBlocking {
                            uri.toString().httpHead().awaitByteArrayResponse()

                        }.second

                        val contentType: String? = resp.header(Headers.CONTENT_TYPE).firstOrNull()
                        contentType?.startsWith("image", true) ?: false
                    }()
        }
        return (couldBe ?: false)
    }

    private fun downloadVerifiedImage(uri: VerifiedImageUri): File {
        lateinit var file: File
        runBlocking {
            uri.toString().httpDownload()
                .fileDestination { _, _ ->
                    File.createTempFile("postiDownload", "").apply { file = this }
                }
                .progress { _, _ -> }
                .awaitByteArrayResponse()
        }
        return file
    }

    fun cacheImageFromWebUri(ctx: Context, uri: Uri): File? {
        return when {
//            TODO("Match insta URLs") -> TODO("Download IG photo")
//            TODO("Match facebook URLs") -> TODO("Download facebook photo")
//            TODO("Match reddit URLs") -> TODO("Download reddit photo")
//            TODO("Match twitter URLs") -> TODO("Download twitter photo")
            couldBeImage(uri) -> downloadVerifiedImage(uri)
            else -> null

        }
    }

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
                        scheme.contains("http", true) -> cacheImageFromWebUri(ctx, uri)
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