package dev.ronova.aethel.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ronova.aethel.data.GameId

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        val key = intent.getStringExtra(ReminderScheduler.EXTRA_KEY) ?: return
        val gameName = intent.getStringExtra(ReminderScheduler.EXTRA_GAME) ?: return
        val game = GameId.entries.firstOrNull { it.name == gameName } ?: return
        val ref = intent.getLongExtra(ReminderScheduler.EXTRA_REF, 0L)
        try {
            ReminderScheduler.fire(context, key, kind, game, ref)
        } finally {
            ReminderScheduler.reschedule(context)
        }
    }
}
