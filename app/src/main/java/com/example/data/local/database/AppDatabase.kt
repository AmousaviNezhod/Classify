package com.example.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CourseEventDao
import com.example.data.local.dao.CourseNoteDao
import com.example.data.local.dao.CourseTaskDao
import com.example.data.local.dao.DownloadedPdfDao
import com.example.data.local.dao.ScheduleDao
import com.example.data.local.dao.ScheduleDayAvailabilityDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.entities.AppSettingsEntity
import com.example.data.local.entities.CourseEventEntity
import com.example.data.local.entities.CourseNoteEntity
import com.example.data.local.entities.CourseTaskEntity
import com.example.data.local.entities.DownloadedPdfEntity
import com.example.data.local.entities.ScheduleClassEntity
import com.example.data.local.entities.ScheduleDayAvailabilityEntity
import com.example.data.local.entities.ScheduleEntity
import kotlinx.coroutines.runBlocking

@Database(
    entities = [
        ScheduleEntity::class,
        ScheduleClassEntity::class,
        DownloadedPdfEntity::class,
        ScheduleDayAvailabilityEntity::class,
        AppSettingsEntity::class,
        CourseEventEntity::class,
        CourseTaskEntity::class,
        CourseNoteEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun downloadedPdfDao(): DownloadedPdfDao
    abstract fun scheduleDayAvailabilityDao(): ScheduleDayAvailabilityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun courseEventDao(): CourseEventDao
    abstract fun courseTaskDao(): CourseTaskDao
    abstract fun courseNoteDao(): CourseNoteDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "unischedule_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            INSTANCE?.let { return it }
            return synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext

            // Snapshot user settings before Room touches the (possibly stale) file,
            // so they can be restored if the database has to be recreated.
            val preservedSettings = readSettingsSafe(appContext)

            fun create(): AppDatabase = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

            var database = create()

            // Self-heal: force the database open right away. If the on-disk schema
            // no longer matches this build (e.g. the schema changed between builds
            // without bumping the version), Room throws IllegalStateException at
            // startup. Instead of crashing on every launch, delete the incompatible
            // database and recreate it from scratch.
            try {
                database.openHelper.writableDatabase
            } catch (e: Exception) {
                Log.e(TAG, "Incompatible database detected, recreating it", e)
                try {
                    database.close()
                } catch (closeError: Exception) {
                    Log.w(TAG, "Failed to close stale database", closeError)
                }
                appContext.deleteDatabase(DB_NAME)
                database = create()
                database.openHelper.writableDatabase
            }

            restoreSettings(database, preservedSettings)
            return database
        }

        private fun readSettingsSafe(context: Context): Map<String, String> {
            return try {
                val file = context.getDatabasePath(DB_NAME)
                if (!file.exists()) return emptyMap()

                SQLiteDatabase.openDatabase(
                    file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    db.query(
                        "app_settings",
                        arrayOf("key", "value"),
                        null, null, null, null, null
                    ).use { cursor ->
                        buildMap {
                            while (cursor.moveToNext()) {
                                put(cursor.getString(0), cursor.getString(1))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Old/incompatible schema without the settings table: nothing to keep.
                emptyMap()
            }
        }

        private fun restoreSettings(database: AppDatabase, settings: Map<String, String>) {
            if (settings.isEmpty()) return
            try {
                runBlocking {
                    val dao = database.settingsDao()
                    for ((key, value) in settings) {
                        if (dao.getSettingDirect(key) == null) {
                            dao.setSetting(AppSettingsEntity(key, value))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore preserved settings", e)
            }
        }
    }
}
