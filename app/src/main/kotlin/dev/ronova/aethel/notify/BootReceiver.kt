package dev.ronova.aethel.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Перезаводит будильники после перезагрузки, обновления приложения или смены времени/часового пояса. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.reschedule(context)
    }
}
