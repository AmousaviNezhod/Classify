package com.example.domain.parser

import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import org.json.JSONArray
import org.json.JSONObject

object JsonScheduleParser {

    /**
     * User's registered academic schedule in standard unit-selection format.
     */
    val DEFAULT_INPUT_JSON: String = """
    {
      "format": "unit-selection-schedule",
      "version": 1,
      "courses": [
        {
          "code": "415007",
          "group": "1",
          "name": "فارسی",
          "units": 3,
          "professor": "رویا یداللهی شاه راه",
          "schedule": [
            {
              "day": "سه‌شنبه",
              "start": "12:00",
              "end": "14:00"
            },
            {
              "day": "شنبه",
              "start": "16:00",
              "end": "18:00",
              "parity": "زوج"
            }
          ]
        },
        {
          "code": "315012",
          "group": "3",
          "name": "برنامه نویسی مبتنی بروب",
          "units": 2,
          "professor": "سمیه مقدم زاده کاشانی",
          "schedule": [
            {
              "day": "پنجشنبه",
              "start": "14:00",
              "end": "16:00",
              "parity": "زوج"
            },
            {
              "day": "یکشنبه",
              "start": "16:00",
              "end": "19:00"
            }
          ]
        },
        {
          "code": "315020",
          "group": "1",
          "name": "اصول و فناوری مذاکره",
          "units": 2,
          "professor": "ندا نمائی قاسمی",
          "schedule": [
            {
              "day": "سه‌شنبه",
              "start": "10:00",
              "end": "12:00"
            }
          ]
        },
        {
          "code": "315008",
          "group": "2",
          "name": "پایگاه داده ها",
          "units": 2,
          "professor": "جواد یزدانجو",
          "schedule": [
            {
              "day": "یکشنبه",
              "start": "14:00",
              "end": "16:00"
            }
          ]
        },
        {
          "code": "315011",
          "group": "2",
          "name": "برنامه نویسی موبایل2",
          "units": 2,
          "professor": "منا مرادی",
          "schedule": [
            {
              "day": "پنجشنبه",
              "start": "14:00",
              "end": "16:00",
              "parity": "فرد"
            },
            {
              "day": "پنجشنبه",
              "start": "11:00",
              "end": "14:00"
            }
          ]
        },
        {
          "code": "315022",
          "group": "2",
          "name": "مباحث ویژه دربرنامه نویسی",
          "units": 2,
          "professor": "جواد یزدانجو",
          "schedule": [
            {
              "day": "شنبه",
              "start": "14:00",
              "end": "16:00",
              "parity": "زوج"
            },
            {
              "day": "سه‌شنبه",
              "start": "16:00",
              "end": "19:00"
            }
          ]
        },
        {
          "code": "415057",
          "group": "2",
          "name": "علوم ومعارف دفاع مقدس ومقاومت",
          "units": 2,
          "professor": "جواد بابائی",
          "schedule": [
            {
              "day": "شنبه",
              "start": "18:00",
              "end": "20:00"
            }
          ]
        },
        {
          "code": "415023",
          "group": "8",
          "name": "اخلاق کاربردی",
          "units": 2,
          "professor": "جواد محمدنیا خرقی",
          "schedule": [
            {
              "day": "سه‌شنبه",
              "start": "14:00",
              "end": "16:00"
            }
          ]
        },
        {
          "code": "315017",
          "group": "2",
          "name": "کارگاه شبکه های کامپیوتری",
          "units": 1,
          "professor": "محسن بیگی",
          "schedule": [
            {
              "day": "پنجشنبه",
              "start": "16:00",
              "end": "19:00"
            }
          ]
        }
      ],
      "validations": {
        "capacity": true,
        "duplicate": true,
        "conflict": true,
        "units": true
      }
    }
    """.trimIndent()

    /**
     * Parses a JSON string (supporting unit-selection-schedule, courses arrays, etc.)
     * into a NormalizedSchedule.
     */
    fun parse(jsonString: String, scheduleId: String = "input_schedule"): Result<NormalizedSchedule> {
        return runCatching {
            val trimmed = jsonString.trim()
            val classes = mutableListOf<ScheduleClass>()
            var term = "۱۴۰۵-۱۴۰۶-۱"
            var title = "برنامه انتخاب واحد من"
            var validationInfo: com.example.domain.model.ScheduleValidationInfo? = null

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                term = root.optString("term", root.optString("ترم", term))
                title = root.optString("title", root.optString("عنوان", title))

                val valObj = root.optJSONObject("validations")
                if (valObj != null) {
                    validationInfo = com.example.domain.model.ScheduleValidationInfo(
                        capacity = valObj.optBoolean("capacity", true),
                        duplicate = valObj.optBoolean("duplicate", true),
                        conflict = valObj.optBoolean("conflict", true),
                        units = valObj.optBoolean("units", true)
                    )
                }

                val coursesArray = root.optJSONArray("courses")
                    ?: root.optJSONArray("classes")
                    ?: root.optJSONArray("دروس")
                    ?: root.optJSONArray("برنامه")
                    ?: JSONArray()

                for (i in 0 until coursesArray.length()) {
                    val obj = coursesArray.optJSONObject(i) ?: continue
                    val parsedClasses = parseCourseOrClassObject(obj, scheduleId)
                    classes.addAll(parsedClasses)
                }
            } else if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val parsedClasses = parseCourseOrClassObject(obj, scheduleId)
                    classes.addAll(parsedClasses)
                }
            } else {
                error("فرمت JSON نامعتبر است.")
            }

            NormalizedSchedule(
                id = scheduleId,
                title = title,
                term = term,
                sourceUrl = "local_json",
                updatedAt = System.currentTimeMillis(),
                classes = classes.distinctBy(ScheduleOfferingIdentity::key),
                rawJson = jsonString,
                validationInfo = validationInfo
            )
        }
    }

    /** Serialize normalized sessions using the Unit Selection-compatible schedule format. */
    fun toUnitSelectionJson(schedule: NormalizedSchedule): String {
        val courses = JSONArray()
        schedule.classes.distinctBy(ScheduleOfferingIdentity::key).forEach { session ->
            courses.put(JSONObject().apply {
                put("code", session.courseCode)
                put("group", session.groupCode)
                put("name", session.courseName)
                put("units", session.units)
                put("professor", session.teacher)
                put("schedule", JSONArray().put(JSONObject().apply {
                    put("day", session.dayOfWeek)
                    put("start", session.startTime)
                    put("end", session.endTime)
                    put("classroom", session.classroom)
                    if (session.parity.isNotBlank()) put("parity", session.parity)
                }))
            })
        }
        return JSONObject().apply {
            put("format", "unit-selection-schedule")
            put("version", 1)
            put("term", schedule.term)
            put("title", schedule.title)
            put("courses", courses)
            schedule.validationInfo?.let { validation ->
                put("validations", JSONObject().apply {
                    put("capacity", validation.capacity)
                    put("duplicate", validation.duplicate)
                    put("conflict", validation.conflict)
                    put("units", validation.units)
                })
            }
        }.toString(2)
    }

    private fun parseCourseOrClassObject(obj: JSONObject, scheduleId: String): List<ScheduleClass> {
        val courseName = obj.optString("name", obj.optString("courseName", obj.optString("course", obj.optString("نام درس", ""))))
        if (courseName.isBlank()) return emptyList()

        val courseCode = obj.optString("code", obj.optString("courseCode", obj.optString("کد درس", "")))
        val group = obj.optString("group", obj.optString("groupCode", obj.optString("گروه", "1")))
        val teacher = obj.optString("professor", obj.optString("teacher", obj.optString("instructor", obj.optString("استاد", ""))))
        val units = obj.optInt("units", obj.optInt("واحد", 2))
        val courseClassroom = obj.optString("classroom", obj.optString("room", obj.optString("کلاس", "")))
        val notes = obj.optString("notes", obj.optString("توضیحات", ""))

        val scheduleArray = obj.optJSONArray("schedule") ?: obj.optJSONArray("برنامه")
        if (scheduleArray != null && scheduleArray.length() > 0) {
            val list = mutableListOf<ScheduleClass>()
            for (j in 0 until scheduleArray.length()) {
                val sessObj = scheduleArray.optJSONObject(j) ?: continue
                val rawDay = sessObj.optString("day", sessObj.optString("روز", "شنبه"))
                val (normalizedDay, dayIndex) = PersianTextNormalizer.normalizeDay(rawDay)
                val start = sessObj.optString("start", sessObj.optString("startTime", sessObj.optString("ساعت شروع", "08:00")))
                val end = sessObj.optString("end", sessObj.optString("endTime", sessObj.optString("ساعت پایان", "10:00")))
                val parity = sessObj.optString("parity", sessObj.optString("هفته", ""))
                val sessionClassroom = sessObj.optString("classroom", sessObj.optString("room", ""))

                list.add(
                    ScheduleClass(
                        id = "${scheduleId}_${classesIdCounter++}",
                        scheduleId = scheduleId,
                        courseName = PersianTextNormalizer.normalizeCourseName(courseName),
                        courseCode = PersianTextNormalizer.toAsciiDigits(courseCode),
                        teacher = PersianTextNormalizer.normalizeText(teacher),
                        dayOfWeek = normalizedDay,
                        dayIndex = dayIndex,
                        startTime = PersianTextNormalizer.normalizeTime(start),
                        endTime = PersianTextNormalizer.normalizeTime(end),
                        classroom = PersianTextNormalizer.normalizeText(sessionClassroom.ifBlank { courseClassroom }),
                        groupCode = PersianTextNormalizer.toAsciiDigits(group).ifEmpty { "1" },
                        parity = PersianTextNormalizer.normalizeText(parity),
                        units = units,
                        notes = PersianTextNormalizer.normalizeText(notes)
                    )
                )
            }
            return list
        }

        // Single session fallback
        val rawDay = obj.optString("day", obj.optString("روز", "شنبه"))
        val (normalizedDay, dayIndex) = PersianTextNormalizer.normalizeDay(rawDay)

        var startTime = obj.optString("startTime", obj.optString("start", obj.optString("ساعت شروع", "")))
        var endTime = obj.optString("endTime", obj.optString("end", obj.optString("ساعت پایان", "")))
        val combinedTime = obj.optString("time", obj.optString("ساعت", obj.optString("زمان", "")))
        if (combinedTime.isNotBlank() && (startTime.isBlank() || endTime.isBlank())) {
            val extracted = PersianTextNormalizer.extractTimeRange(combinedTime)
            if (extracted != null) {
                startTime = extracted.first
                endTime = extracted.second
            }
        }
        val parity = obj.optString("parity", obj.optString("هفته", ""))
        val classroom = obj.optString("classroom", obj.optString("room", obj.optString("کلاس", "")))

        return listOf(
            ScheduleClass(
                id = "${scheduleId}_${classesIdCounter++}",
                scheduleId = scheduleId,
                courseName = PersianTextNormalizer.normalizeCourseName(courseName),
                courseCode = PersianTextNormalizer.toAsciiDigits(courseCode),
                teacher = PersianTextNormalizer.normalizeText(teacher),
                dayOfWeek = normalizedDay,
                dayIndex = dayIndex,
                startTime = PersianTextNormalizer.normalizeTime(startTime),
                endTime = PersianTextNormalizer.normalizeTime(endTime),
                classroom = PersianTextNormalizer.normalizeText(classroom),
                groupCode = PersianTextNormalizer.toAsciiDigits(group).ifEmpty { "1" },
                parity = PersianTextNormalizer.normalizeText(parity),
                units = units,
                notes = PersianTextNormalizer.normalizeText(notes)
            )
        )
    }

    private var classesIdCounter = 1L
}
