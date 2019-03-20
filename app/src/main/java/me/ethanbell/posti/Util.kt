package me.ethanbell.posti

import android.content.*
import android.content.Intent.ACTION_SEND
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import java.io.File

object Util {
    fun prepImage(ctx: Context, uri: Uri) {
        val intent = Intent("postiPROCESSIMG", uri)
        startActivity(ctx, intent, Bundle.EMPTY)
    }

    fun postImage(ctx: Context, uri: Uri) {
        val instaPost = Intent(ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri)
            .setFlags(FLAG_GRANT_READ_URI_PERMISSION).setPackage("com.instagram.android")
        startActivity(ctx, instaPost, Bundle.EMPTY)
    }

    fun cacheImageFromClip(ctx: Context, clip: ClipData?): Uri? {
        return clip?.itemCount?.let { count ->
            val item = clip.getItemAt(0)
            val type = clip.description.getMimeType(0)!!
            when { // Generate a preliminary (nonlocal) URI
                type.contains("image/") -> {
                    //TODO("Clipboard reported an image, but not sure how to handle this. Aborting for now.")
                    null
                }
                type.contentEquals(ClipDescription.MIMETYPE_TEXT_URILIST) -> item.uri
                type.contentEquals(ClipDescription.MIMETYPE_TEXT_PLAIN) -> item.text.toString().runCatching {
                    Uri.parse(
                        this
                    )
                }.getOrNull()
                else -> null
            }?.normalizeScheme()?.let { uri -> // Convert the URI into something controlled by the application - a Uri for a temporary file
                uri.scheme?.let { scheme ->
                    when {
                        scheme.contains("http") -> TODO("Download to temp file")
                        scheme.contentEquals(ContentResolver.SCHEME_CONTENT) ||
                                scheme.contentEquals(ContentResolver.SCHEME_FILE) -> {
                            ctx.contentResolver.openInputStream(uri)?.let { inStr ->
                                // copy local file to an application-controlled temporary file
                                val file = File.createTempFile("postiLocalCopy", "").apply {
                                    writeBytes(inStr.readBytes())
                                    inStr.close()
                                }
                                FileProvider.getUriForFile(ctx, "me.ethanbell.posti.fileprovider", file)
                            }


                        }
                        else -> null
                    }
                }
            }
        }
    }
}