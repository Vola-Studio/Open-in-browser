package xyz.vola.openinbrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import java.lang.Exception


class ClipboardListener : Service() {
    private val listener = OnPrimaryClipChangedListener { performClipboardCheck() }
    private val tag = "ClipboardListener"
    private var isSystemUser = true

    override fun onCreate() {
        Log.d(tag, "Created")
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(listener)
        createNotificationChannel()
        startForeground(1, createNotification())
        if (!(getSystemService(UserManager::class.java)).isSystemUser) {
            isSystemUser = false
        }
    }

    private fun createNotificationChannel() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = getString(R.string.notification_background_channel)
        val descriptionText = getString(R.string.notification_background_channel_description)
        val importance = NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel(tag, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() =
        Notification.Builder(this, tag)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_background_channel_description))
            // Don't do it. Let it be broken. Let system generate one.
            // .setSmallIcon(R.drawable.notification_icon_background)
            .build()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun performClipboardCheck() {
        if (!isSystemUser) return
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboardManager.hasPrimaryClip()) {
            val clipData = clipboardManager.primaryClip
            if (clipData!!.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val text = clipData.getItemAt(0).text.toString()
                val url = URLGuess(this, text)
                if (url != null) {
                    OpenInBrowserNotification.notify(this, Uri.parse(url))
                }
            }
        }
    }
}
