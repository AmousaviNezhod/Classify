package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DaySelector(
    selectedDayIndex: Int?,
    onSelectDay: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        null to "همه روزها",
        0 to "شنبه",
        1 to "یکشنبه",
        2 to "دوشنبه",
        3 to "سه‌شنبه",
        4 to "چهارشنبه",
        5 to "پنج‌شنبه",
        6 to "جمعه"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (index, title) ->
            val isSelected = selectedDayIndex == index
            FilterChip(
                selected = isSelected,
                onClick = { onSelectDay(index) },
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("day_chip_${index ?: -1}")
            )
        }
    }
}
