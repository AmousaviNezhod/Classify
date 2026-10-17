package com.example

import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.ui.upcoming.EventStatusFilter
import com.example.ui.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCompletionTrackingTest {

    private fun event(
        id: String,
        title: String,
        timestamp: Long,
        isCompleted: Boolean = false,
        type: CourseEventType = CourseEventType.ASSIGNMENT
    ) = CourseEvent(
        id = id,
        title = title,
        timestamp = timestamp,
        isCompleted = isCompleted,
        type = type,
        dateString = "1403/10/25",
        timeString = "10:00"
    )

    @Test
    fun `toggle completion correctly reverses isCompleted flag`() {
        val original = event("1", "تمرین شماره ۱", System.currentTimeMillis() + 100000, isCompleted = false)
        val completed = original.copy(isCompleted = !original.isCompleted)
        assertTrue(completed.isCompleted)

        val reopened = completed.copy(isCompleted = !completed.isCompleted)
        assertFalse(reopened.isCompleted)
        assertEquals(original.id, reopened.id)
        assertEquals(original.title, reopened.title)
    }

    @Test
    fun `status filtering partitions upcoming, overdue, and completed events correctly`() {
        val now = System.currentTimeMillis()
        val upcomingEvent = event("1", "پروژه آینده", now + 86400_000L, isCompleted = false)
        val overdueEvent = event("2", "تمرین گذشته", now - 86400_000L, isCompleted = false)
        val completedPastEvent = event("3", "امتحان انجام شده", now - 172800_000L, isCompleted = true)
        val completedFutureEvent = event("4", "ارائه تکمیل شده", now + 172800_000L, isCompleted = true)

        val allEvents = listOf(upcomingEvent, overdueEvent, completedPastEvent, completedFutureEvent)

        // Filter: UPCOMING (not completed and not overdue)
        val upcomingList = allEvents.filter { !it.isCompleted && !DateTimeUtils.isOverdue(it.timestamp) }
        assertEquals(1, upcomingList.size)
        assertEquals("پروژه آینده", upcomingList.first().title)

        // Filter: OVERDUE (not completed and overdue)
        val overdueList = allEvents.filter { !it.isCompleted && DateTimeUtils.isOverdue(it.timestamp) }
        assertEquals(1, overdueList.size)
        assertEquals("تمرین گذشته", overdueList.first().title)

        // Filter: COMPLETED (isCompleted == true)
        val completedList = allEvents.filter { it.isCompleted }
        assertEquals(2, completedList.size)
        assertTrue(completedList.any { it.title == "امتحان انجام شده" })
        assertTrue(completedList.any { it.title == "ارائه تکمیل شده" })
    }
}
