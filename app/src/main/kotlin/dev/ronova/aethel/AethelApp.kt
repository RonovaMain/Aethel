package dev.ronova.aethel

import android.app.Application
import android.content.Context
import dev.ronova.aethel.data.Repository
import dev.ronova.aethel.notify.Notifier
import dev.ronova.aethel.notify.ReminderScheduler

class AethelApp : Application() {

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
        Notifier.createChannel(this)
        ReminderScheduler.reschedule(this)
    }
}

/** Быстрый доступ к хранилищу из любого Context. */
fun Context.repo(): Repository = (applicationContext as AethelApp).repository
