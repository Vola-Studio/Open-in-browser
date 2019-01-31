package xyz.vola.openinbrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.UserManager
import android.util.Log
import android.widget.Toast

const val REQUEST_CROSS_PROFILE = "xyz.vola.openinbrowser.REQUEST_CROSS_PROFILE"
const val RECEIVE_CROSS_PROFILE = "xyz.vola.openinbrowser.RECEIVE_CROSS_PROFILE"
class CrossProfileHelper : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val userManager = context.getSystemService(UserManager::class.java)
        Log.d("Cross-Helper", intent.data?.toString() ?: "void url")
        Log.d("Cross-Helper", intent.action ?: "action")
        Log.d("Cross-Helper", if (userManager.isSystemUser) "User 0" else "User 10")
        OpenInBrowserNotification.cancel(context)
        when (intent.action) {
            REQUEST_CROSS_PROFILE -> {
                for (user in userManager.userProfiles) {
                    context.sendBroadcastAsUser(intent.apply { intent.action = RECEIVE_CROSS_PROFILE }, user)
                }
            }
            RECEIVE_CROSS_PROFILE -> {
                if (!userManager.isSystemUser) {
                    Toast.makeText(context, R.string.notification_open_in_managed_profile, Toast.LENGTH_SHORT)
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = intent.data
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }
    }
}
