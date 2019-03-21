package me.ethanbell.posti

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Picture
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_process_image.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Math.abs
import java.lang.Math.max
import kotlin.math.roundToInt

class ProcessImageActivity : AppCompatActivity() {
    private lateinit var originalBitmap: Bitmap
    lateinit var bitmap: Bitmap
    var squared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_process_image)
        val uri = intent?.data ?: Util.cacheImageFromClip(this, intent?.clipData)
        uri?.let {

            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            val orientMatrix: Matrix = Matrix().apply {
                val inStr = contentResolver.openInputStream(uri)
                when (ExifInterface(inStr).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    else -> Unit
                }
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, orientMatrix, false)
            if (bitmap.height > 1080 && bitmap.width < bitmap.height) { // If the image is too tall
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (1080f * bitmap.width.toFloat() / bitmap.height.toFloat()).roundToInt(), 1080, false
                )
            } else if (bitmap.width > 1080) { // If the image is too wide
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    1080, (1080f * bitmap.height.toFloat() / bitmap.width.toFloat()).roundToInt(), false
                )
            }
            imageView.setImageBitmap(bitmap)
            originalBitmap = bitmap.copy(bitmap.config, false)
        }
    }

    fun onFormat(v: View): Unit {
        squared = !squared
        if (squared && bitmap.height != bitmap.width) { // square image
            val squareDim = max(bitmap.height, bitmap.width)
            val paddingRequired = abs(bitmap.height - bitmap.width) / 2f
            val newBitmap = Bitmap.createBitmap(squareDim, squareDim, bitmap.config)
            Canvas(newBitmap).apply {
                drawColor(android.graphics.Color.WHITE)
                if (bitmap.height > bitmap.width) {
                    drawBitmap(bitmap, paddingRequired, 0f, null)
                } else {
                    drawBitmap(bitmap, 0f, paddingRequired, null)
                }
            }
            bitmap = newBitmap.copy(originalBitmap.config, false)
        } else { // reset image
            bitmap = originalBitmap.copy(originalBitmap.config, false)
        }
        imageView.setImageBitmap(bitmap)
    }

    fun onPost(v: View): Unit {
        val image = File.createTempFile("postiImage", ".png")
        val os = image.outputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        os.flush()
        os.close()
        Util.postImage(
            this,
            FileProvider.getUriForFile(this, "me.ethanbell.posti.fileprovider", image)
        )
    }
}