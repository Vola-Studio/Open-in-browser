package xyz.vola.openinbrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

object OpenInBrowserNotification {
    private const val NOTIFICATION_TAG = "OpenInBrowser"

    /**
     * Shows the notification, or updates a previously shown notification of
     * this type, with the given parameters.
     *
     * @see .cancel
     */
    fun notify(
        context: Context,
        url: Uri
    ) {
        createNotificationChannel(context)
        val res = context.resources

        val title = res.getString(R.string.notification_open_in_browser_title)
        val text = res.getString(R.string.notification_open_in_browser_text, url)
        val builder = Notification.Builder(context, NOTIFICATION_TAG)
            .setSmallIcon(R.drawable.ic_stat_open_in_browser)
            .setContentTitle(title)
            .setContentText(text)
            // 可访问性标题
            .setTicker(title)

            .setContentIntent(createNormalOpenIntent(context, url))
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        context,
                        R.drawable.ic_notification_action_open_in_private
                    ),
                    res.getString(R.string.notification_action_open_in_private),
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, DialogContainer::class.java).apply {
                            putExtra(OPEN_DIALOG, true)
                            data = url
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                ).build()
            )
            // Automatically dismiss the notification when it is touched.
            .setAutoCancel(true)
            .setTimeoutAfter(30 * 1000)

        notify(context, builder.build())
    }

    private fun notify(context: Context, notification: Notification) {
        with(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(NOTIFICATION_TAG.hashCode(), notification)
        }
    }

    /**
     * 取消通知
     */
    fun cancel(context: Context) {
        with(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            cancel(NOTIFICATION_TAG.hashCode())
        }
    }

    private fun createNotificationChannel(context: Context) {
        with(context) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_TAG, name, importance).apply {
                description = descriptionText
                vibrationPattern = longArrayOf(0)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNormalOpenIntent(context: Context, url: Uri): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, url),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
