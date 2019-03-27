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
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity



class MainActivity : AppCompatActivity() {

    val IMAGE_FROM_GALLERY_SELECTED = 1002;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reddit.setup(applicationContext)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_ossl -> {
                OssLicensesMenuActivity.setActivityTitle("Open Source Licenses")
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        val toast = Toast.makeText(applicationContext, "Checking clipboard for a postable image", Toast.LENGTH_LONG)
        toast.show()
        GlobalScope.launch {
            Util.cacheImageFromClip(applicationContext, clipboard.primaryClip)?.let {
                toast.cancel()
                Util.prepImage(applicationContext, it)
            }
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
