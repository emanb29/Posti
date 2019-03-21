package me.ethanbell.posti

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    val IMAGE_FROM_GALLERY_SELECTED = 1002;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onServiceToggle(v: View){
        if (ClipboardMonitorService.isRunning) stopService(Intent(this, ClipboardMonitorService::class.java))
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ClipboardMonitorService::class.java))
        } else {
            startService(Intent(this, ClipboardMonitorService::class.java))
        }
    }

    fun onSelectFromClipboard(v: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Util.cacheImageFromClip(this, clipboard.primaryClip)?.let {
            Util.prepImage(this, it)
        }
    }


    fun onSelectFromGallery(v: View) {
        val intent: Intent = Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), IMAGE_FROM_GALLERY_SELECTED)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IMAGE_FROM_GALLERY_SELECTED -> {
                if (resultCode == Activity.RESULT_CANCELED) {
                    println("User cancelled image selection")
                }
                else if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { Util.prepImage(this, it) }
                }

            }
        }
    }


}
