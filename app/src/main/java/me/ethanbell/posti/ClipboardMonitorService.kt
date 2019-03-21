package me.ethanbell.posti

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.widget.Toast

class ClipboardMonitorService : Service() {
    lateinit var clipboard: ClipboardManager
    lateinit var notifManager: NotificationManager

    private var startMode: Int = 0             // indicates how to behave if the service is killed
    private var binder: IBinder? = null        // interface for clients that bind
    private var allowRebind: Boolean = false   // indicates whether onRebind should be useds

    lateinit var serviceNotification: Notification

    companion object {
        val channelId = "me.ethanbell.posti.lowPriNotifs"
        val notifId = 9091 // arbitrary
        var isRunning: Boolean = false

    }

    override fun onCreate() {

        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Posti Service Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Click this to open the last copied image in Posti"
            }
            notifManager.createNotificationChannel(channel)
        }

        serviceNotification = (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(this, channelId)
                } else {
                    Notification.Builder(this)
                })
            .setContentTitle("Posti Clipboard Service")
            .setContentText("Posti is monitoring clipboard for postable images")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(Intent(this, ProcessImageActivity::class.java).let {
                PendingIntent.getActivity(this, 0, it, 0)
            })
            .setTicker("Posti Clipboard Service")
            .setOnlyAlertOnce(true)
            .build()

        startForeground(notifId, serviceNotification)

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(listener)
        isRunning = true

        Toast.makeText(this, "Posti Clipboard Service now running", Toast.LENGTH_SHORT).show()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()
        return startMode
    }

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return allowRebind
    }

    override fun onRebind(intent: Intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
        clipboard.removePrimaryClipChangedListener(listener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.deleteNotificationChannel(channelId)
        }
        isRunning = false
        Toast.makeText(this, "Posti Clipboard Service terminated", Toast.LENGTH_SHORT).show()
    }

    private val listener: () -> Unit = {
        Util.cacheImageFromClip(this, clipboard.primaryClip)?.let { uri ->
            Toast.makeText(applicationContext, "Click Posti Notification to Share", Toast.LENGTH_SHORT).show()
            notifManager.notify(notifId, serviceNotification.apply {
                contentIntent = Intent(applicationContext, ProcessImageActivity::class.java).setData(uri).let {
                    PendingIntent.getActivity(applicationContext, 0, it, 0)
                }
            })
        }
    }

}