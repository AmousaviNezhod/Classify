package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

                if (eventId.isBlank()) return

                val pendingResult = goAsync()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val db = com.example.data.local.database.AppDatabase.getDatabase(context)
                        val eventEntity = db.courseEventDao().getEventById(eventId)
                        if (eventEntity == null || eventEntity.isCompleted || eventEntity.reminderMinutesBefore < 0) {
                            Log.d("ReminderReceiver", "Event $eventId is completed, deleted, or reminder is turned off; skipping notification.")
                            return@launch
                        }

                        val notificationId = (eventId.hashCode() and 0x7FFFFFFF)
                        ReminderNotificationManager.showNotification(
                            context = context,
                            notificationId = notificationId,
                            title = eventEntity.title.ifBlank { title },
                            courseName = eventEntity.courseName.ifBlank { courseName },
                            reminderText = reminderText,
                            typeName = eventEntity.type.ifBlank { typeName }
                        )
                    } catch (e: Exception) {
                        Log.e("ReminderReceiver", "Error validating event before notification", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.MY_PACKAGE_REPLACED" -> {
                ReminderNotificationManager.rescheduleAll(context)
            }
        }
    }
}
