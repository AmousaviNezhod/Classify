package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.database.AppDatabase
import com.example.data.repository.UniversityScheduleRepository
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.JsonScheduleParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SCHEDULE_CHANGED || intent.action == Intent.ACTION_TIME_TICK || intent.action == Intent.ACTION_TIME_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            updateWidgets(context, appWidgetManager, ids)
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val inputJson = db.settingsDao().getSettingDirect(UniversityScheduleRepository.KEY_INPUT_JSON) ?: ""
                val schedule = JsonScheduleParser.parse(inputJson, UniversityScheduleRepository.SCHEDULE_ID_INPUT).getOrNull()
                val catalogClasses = db.scheduleDao().getClassesDirect(UniversityScheduleRepository.SCHEDULE_ID_UNIVERSITY).map { it.toDomain() }
                val enrichedSchedule = if (schedule != null && catalogClasses.isNotEmpty()) {
                    val catalogByDay = catalogClasses.groupBy { it.dayIndex }
                    schedule.copy(
                        classes = schedule.classes.map {
                            ScheduleOfferingIdentity.enrichFromCatalog(it, catalogByDay[it.dayIndex].orEmpty())
                        }
                    )
                } else schedule

                val now = Calendar.getInstance()
                val currentDayIndex = getPersianDayIndex(now.get(Calendar.DAY_OF_WEEK))
                val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                val todayClasses = enrichedSchedule?.classes.orEmpty()
                    .filter { it.dayIndex == currentDayIndex }
                    .sortedBy { it.startTime }

                val nextClass = todayClasses.firstOrNull { it.endTime >= nowTimeStr }
                    ?: todayClasses.firstOrNull()

                val dayNameFa = getDayNameFa(currentDayIndex)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_next_class)

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    views.setTextViewText(R.id.widget_day_text, "امروز · $dayNameFa")

                    if (nextClass != null) {
                        val isHappeningNow = nextClass.startTime <= nowTimeStr && nextClass.endTime >= nowTimeStr
                        views.setTextViewText(
                            R.id.widget_badge,
                            if (isHappeningNow) "در حال برگزاری" else "کلاس بعدی"
                        )
                        views.setTextViewText(R.id.widget_course_title, nextClass.courseName)
                        views.setTextViewText(
                            R.id.widget_time,
                            "${PersianTextNormalizer.toPersianDigits(nextClass.startTime)} – ${PersianTextNormalizer.toPersianDigits(nextClass.endTime)}"
                        )
                        views.setTextViewText(
                            R.id.widget_classroom,
                            nextClass.classroom.ifBlank { "مکان مشخص نشده" }
                        )

                        if (nextClass.teacher.isNotBlank()) {
                            views.setViewVisibility(R.id.widget_teacher, View.VISIBLE)
                            views.setTextViewText(R.id.widget_teacher, "استاد: ${nextClass.teacher}")
                        } else {
                            views.setViewVisibility(R.id.widget_teacher, View.GONE)
                        }

                        val remainingCount = todayClasses.count { it.startTime > nextClass.startTime }
                        val footerText = if (remainingCount > 0) {
                            "${PersianTextNormalizer.toPersianDigits(remainingCount.toString())} کلاس دیگر برای امروز باقی مانده"
                        } else {
                            "آخرین کلاس امروز شما"
                        }
                        views.setTextViewText(R.id.widget_footer, footerText)
                    } else {
                        views.setTextViewText(R.id.widget_badge, "بدون کلاس")
                        views.setTextViewText(R.id.widget_course_title, "امروز کلاسی ندارید")
                        views.setTextViewText(R.id.widget_time, "وقت آزاد")
                        views.setTextViewText(R.id.widget_classroom, "—")
                        views.setViewVisibility(R.id.widget_teacher, View.GONE)
                        views.setTextViewText(R.id.widget_footer, "برای مشاهده برنامهٔ ترم لمس کنید")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_SCHEDULE_CHANGED = "com.example.ACTION_SCHEDULE_CHANGED"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = ACTION_SCHEDULE_CHANGED
            }
            context.sendBroadcast(intent)
        }

        fun getPersianDayIndex(calendarDayOfWeek: Int): Int {
            return when (calendarDayOfWeek) {
                Calendar.SATURDAY -> 0
                Calendar.SUNDAY -> 1
                Calendar.MONDAY -> 2
                Calendar.TUESDAY -> 3
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 5
                Calendar.FRIDAY -> 6
                else -> 0
            }
        }

        fun getDayNameFa(dayIndex: Int): String {
            return when (dayIndex) {
                0 -> "شنبه"
                1 -> "یکشنبه"
                2 -> "دوشنبه"
                3 -> "سه‌شنبه"
                4 -> "چهارشنبه"
                5 -> "پنج‌شنبه"
                6 -> "جمعه"
                else -> ""
            }
        }
    }
}
