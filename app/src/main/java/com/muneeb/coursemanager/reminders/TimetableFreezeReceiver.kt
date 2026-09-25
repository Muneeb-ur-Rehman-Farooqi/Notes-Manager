package com.muneeb.coursemanager.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a timed freeze expires — clears the frozen flag and re-arms every entry. */
class TimetableFreezeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TimetableFreezeManager.unfreeze(context)
            } catch (_: Exception) {
                // best-effort
            } finally {
                pendingResult.finish()
            }
        }
    }
}