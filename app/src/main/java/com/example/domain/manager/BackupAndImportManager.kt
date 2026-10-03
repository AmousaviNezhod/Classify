package com.example.domain.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.CourseNote
import com.example.domain.model.CourseTask
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class FullBackupData(
    val format: String,
    val version: Int,
    val timestamp: Long,
    val sourceUrl: String,
    val themeMode: String,
    val inputScheduleJson: String,
    val events: List<CourseEvent>,
    val tasks: List<CourseTask>,
    val notes: List<CourseNote>
)

object BackupAndImportManager {

    const val FORMAT_EVENTS = "classify-events"
    const val FORMAT_FULL_BACKUP = "classify-full-backup"
    const val CURRENT_VERSION = 1

    fun exportEventsJson(events: List<CourseEvent>): String {
        val root = JSONObject()
        root.put("format", FORMAT_EVENTS)
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val eventsArray = JSONArray()
        events.forEach { event ->
            val obj = JSONObject()
            obj.put("id", event.id)
            obj.put("courseKey", event.courseKey)
            obj.put("courseName", event.courseName)
            obj.put("courseCode", event.courseCode)
            obj.put("title", event.title)
            obj.put("type", event.type.name)
            obj.put("dateString", event.dateString)
            obj.put("timeString", event.timeString)
            obj.put("timestamp", event.timestamp)
            obj.put("description", event.description)
            obj.put("location", event.location)
            obj.put("reminderMinutesBefore", event.reminderMinutesBefore)
            obj.put("isCompleted", event.isCompleted)
            obj.put("createdAt", event.createdAt)
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)
        return root.toString(2)
    }

    fun parseEventsJson(json: String): Result<List<CourseEvent>> = runCatching {
        val root = JSONObject(json)
        val format = root.optString("format", "")
        val eventsArray = if (root.has("events")) root.getJSONArray("events") else JSONArray()

        val list = mutableListOf<CourseEvent>()
        for (i in 0 until eventsArray.length()) {
            val obj = eventsArray.getJSONObject(i)
            list.add(
                CourseEvent(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    courseKey = obj.optString("courseKey", ""),
                    courseName = obj.optString("courseName", ""),
                    courseCode = obj.optString("courseCode", ""),
                    title = obj.getString("title"),
                    type = CourseEventType.fromString(obj.optString("type", "OTHER")),
                    dateString = obj.optString("dateString", ""),
                    timeString = obj.optString("timeString", ""),
                    timestamp = obj.optLong("timestamp", 0L),
                    description = obj.optString("description", ""),
                    location = obj.optString("location", ""),
                    reminderMinutesBefore = obj.optInt("reminderMinutesBefore", -1),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        list
    }

    fun createFullBackup(
        sourceUrl: String,
        themeMode: String,
        inputScheduleJson: String,
        events: List<CourseEvent>,
        tasks: List<CourseTask>,
        notes: List<CourseNote>
    ): String {
        val root = JSONObject()
        root.put("format", FORMAT_FULL_BACKUP)
        root.put("version", CURRENT_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("sourceUrl", sourceUrl)
        root.put("themeMode", themeMode)
        root.put("inputScheduleJson", inputScheduleJson)

        val eventsArray = JSONArray()
        events.forEach { event ->
            val obj = JSONObject()
            obj.put("id", event.id)
            obj.put("courseKey", event.courseKey)
            obj.put("courseName", event.courseName)
            obj.put("courseCode", event.courseCode)
            obj.put("title", event.title)
            obj.put("type", event.type.name)
            obj.put("dateString", event.dateString)
            obj.put("timeString", event.timeString)
            obj.put("timestamp", event.timestamp)
            obj.put("description", event.description)
            obj.put("location", event.location)
            obj.put("reminderMinutesBefore", event.reminderMinutesBefore)
            obj.put("isCompleted", event.isCompleted)
            obj.put("createdAt", event.createdAt)
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)

        val tasksArray = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("courseKey", task.courseKey)
            obj.put("courseName", task.courseName)
            obj.put("title", task.title)
            obj.put("isDone", task.isDone)
            obj.put("deadlineString", task.deadlineString)
            obj.put("deadlineTimestamp", task.deadlineTimestamp)
            obj.put("createdAt", task.createdAt)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val notesArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("courseKey", note.courseKey)
            obj.put("courseName", note.courseName)
            obj.put("content", note.content)
            obj.put("createdAt", note.createdAt)
            obj.put("updatedAt", note.updatedAt)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        return root.toString(2)
    }

    fun parseFullBackup(json: String): Result<FullBackupData> = runCatching {
        val root = JSONObject(json)
        val format = root.optString("format", "")
        if (format != FORMAT_FULL_BACKUP && !root.has("inputScheduleJson") && !root.has("events")) {
            error("قالب فایل پشتیبان نامعتبر است.")
        }

        val version = root.optInt("version", 1)
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())
        val sourceUrl = root.optString("sourceUrl", "")
        val themeMode = root.optString("themeMode", "SYSTEM")
        val inputScheduleJson = root.optString("inputScheduleJson", "")

        val eventsList = mutableListOf<CourseEvent>()
        if (root.has("events")) {
            val eventsArr = root.getJSONArray("events")
            for (i in 0 until eventsArr.length()) {
                val obj = eventsArr.getJSONObject(i)
                eventsList.add(
                    CourseEvent(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        courseKey = obj.optString("courseKey", ""),
                        courseName = obj.optString("courseName", ""),
                        courseCode = obj.optString("courseCode", ""),
                        title = obj.getString("title"),
                        type = CourseEventType.fromString(obj.optString("type", "OTHER")),
                        dateString = obj.optString("dateString", ""),
                        timeString = obj.optString("timeString", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        description = obj.optString("description", ""),
                        location = obj.optString("location", ""),
                        reminderMinutesBefore = obj.optInt("reminderMinutesBefore", -1),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val tasksList = mutableListOf<CourseTask>()
        if (root.has("tasks")) {
            val tasksArr = root.getJSONArray("tasks")
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.getJSONObject(i)
                tasksList.add(
                    CourseTask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        courseKey = obj.optString("courseKey", ""),
                        courseName = obj.optString("courseName", ""),
                        title = obj.getString("title"),
                        isDone = obj.optBoolean("isDone", false),
                        deadlineString = obj.optString("deadlineString", ""),
                        deadlineTimestamp = obj.optLong("deadlineTimestamp", 0L),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val notesList = mutableListOf<CourseNote>()
        if (root.has("notes")) {
            val notesArr = root.getJSONArray("notes")
            for (i in 0 until notesArr.length()) {
                val obj = notesArr.getJSONObject(i)
                notesList.add(
                    CourseNote(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        courseKey = obj.optString("courseKey", ""),
                        courseName = obj.optString("courseName", ""),
                        content = obj.getString("content"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        FullBackupData(
            format = format,
            version = version,
            timestamp = timestamp,
            sourceUrl = sourceUrl,
            themeMode = themeMode,
            inputScheduleJson = inputScheduleJson,
            events = eventsList,
            tasks = tasksList,
            notes = notesList
        )
    }

    fun shareJsonFile(
        context: Context,
        content: String,
        fileName: String,
        chooserTitle: String
    ): Boolean {
        return try {
            val exportsDir = File(context.cacheDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()

            val file = File(exportsDir, fileName)
            file.writeText(content, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
