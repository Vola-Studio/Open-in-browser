package xyz.vola.openinbrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startForegroundService

class AutoStartService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        with(context) {
            Log.d("AutoStart", "Starting")
            startForegroundService(this, Intent(this, ClipboardListener::class.java))
        }
    }
}