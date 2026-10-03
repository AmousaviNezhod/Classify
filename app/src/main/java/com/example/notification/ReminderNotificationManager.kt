package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.database.AppDatabase
import com.example.domain.model.CourseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderNotificationManager {

    private const val TAG = "ReminderManager"
    const val CHANNEL_ID = "course_reminders"
    const val CHANNEL_NAME = "یادآور کلاس‌ها و رویدادها"
    const val ACTION_REMINDER = "com.example.ACTION_EVENT_REMINDER"

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_COURSE_NAME = "extra_course_name"
    const val EXTRA_REMINDER_TEXT = "extra_reminder_text"
    const val EXTRA_TYPE = "extra_type"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌ها و یادآورهای امتحانات، ارائه‌ها و تکالیف"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, event: CourseEvent) {
        if (event.reminderMinutesBefore < 0 || event.isCompleted || event.timestamp <= 0L) {
            cancelReminder(context, event.id)
            return
        }

        val triggerTime = event.timestamp - (event.reminderMinutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()
        if (triggerTime <= now) {
            Log.d(TAG, "Reminder trigger time is in the past for event ${event.id}")
            return
        }

        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_TITLE, event.title)
            putExtra(EXTRA_COURSE_NAME, event.courseName)
            putExtra(EXTRA_REMINDER_TEXT, formatReminderDescription(event))
            putExtra(EXTRA_TYPE, event.type.name)
        }

        val requestCode = event.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder for ${event.title} at $triggerTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for event ${event.id}", e)
        }
    }

    fun cancelReminder(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val requestCode = eventId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val events = db.courseEventDao().getAllEventsDirect()
                val now = System.currentTimeMillis()
                events.forEach { entity ->
                    val event = CourseEvent.fromEntity(entity)
                    if (event.reminderMinutesBefore >= 0 && !event.isCompleted && event.timestamp > now) {
                        scheduleReminder(context, event)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders", e)
            }
        }
    }

    private fun formatReminderDescription(event: CourseEvent): String {
        return when (event.reminderMinutesBefore) {
            0 -> "اکنون زمان ${event.type.titleFa} است."
            60 -> "۱ ساعت دیگر"
            1440 -> "فردا (۱ روز مانده)"
            2880 -> "۲ روز دیگر"
            4320 -> "۳ روز دیگر"
            10080 -> "۱ هفته دیگر (۷ روز مانده)"
            else -> "${event.reminderMinutesBefore / 60} ساعت دیگر"
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        courseName: String,
        reminderText: String,
        typeName: String
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val headerText = if (courseName.isNotBlank()) "$title — $courseName" else title
        val subText = if (reminderText.isNotBlank()) reminderText else "یادآور رویداد دانشگاهی"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(headerText)
            .setContentText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$headerText\n$subText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
}
