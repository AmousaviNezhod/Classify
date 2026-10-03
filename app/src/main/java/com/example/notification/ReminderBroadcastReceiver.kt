package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("ReminderReceiver", "Received action: $action")

        when (action) {
            ReminderNotificationManager.ACTION_REMINDER -> {
                val eventId = intent.getStringExtra(ReminderNotificationManager.EXTRA_EVENT_ID) ?: ""
                val title = intent.getStringExtra(ReminderNotificationManager.EXTRA_TITLE) ?: "رویداد دانشگاهی"
                val courseName = intent.getStringExtra(ReminderNotificationManager.EXTRA_COURSE_NAME) ?: ""
                val reminderText = intent.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_TEXT) ?: ""
                val typeName = intent.getStringExtra(ReminderNotificationManager.EXTRA_TYPE) ?: "OTHER"

                val notificationId = (eventId.hashCode() and 0x7FFFFFFF)
                ReminderNotificationManager.showNotification(
                    context = context,
                    notificationId = notificationId,
                    title = title,
                    courseName = courseName,
                    reminderText = reminderText,
                    typeName = typeName
                )
            }
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.MY_PACKAGE_REPLACED" -> {
                ReminderNotificationManager.rescheduleAll(context)
            }
        }
    }
}
