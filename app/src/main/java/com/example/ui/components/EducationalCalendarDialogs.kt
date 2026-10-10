package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calendar.PersianCalendarHelper
import com.example.domain.normalizer.PersianTextNormalizer

@Composable
fun EducationalWeekEditDialog(
    currentWeekNumber: Int,
    onDismiss: () -> Unit,
    onSave: (newWeekNumber: Int) -> Unit
) {
    var weekNumber by remember { mutableIntStateOf(currentWeekNumber.coerceAtLeast(1)) }
    val parity = PersianCalendarHelper.getWeekParityString(weekNumber)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "تنظیم هفته آموزشی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "شماره هفته آموزشی جاری را تعیین کنید تا محاسبه زوج یا فرد بودن کلاس‌ها بر اساس آن انجام شود:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (weekNumber > 1) weekNumber-- },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کاهش")
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = PersianTextNormalizer.toPersianDigits(weekNumber.toString()),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (weekNumber < 30) weekNumber++ },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزایش")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (PersianCalendarHelper.isEvenWeek(weekNumber))
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "وضعیت: هفته $parity",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (PersianCalendarHelper.isEvenWeek(weekNumber))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(weekNumber) }
            ) {
                Text("ثبت و ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDatePickerDialog(
    initialJdn: Long,
    onDismiss: () -> Unit,
    onDateSelected: (newJdn: Long) -> Unit
) {
    val initialDate = remember(initialJdn) { PersianCalendarHelper.jdnToJalali(initialJdn) }
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember { mutableIntStateOf(initialDate.day) }

    val years = remember { listOf(1403, 1404, 1405, 1406) }
    val maxDays = remember(selectedMonth) {
        when {
            selectedMonth in 1..6 -> 31
            selectedMonth in 7..11 -> 30
            else -> 29 // اسفند (ساده)
        }
    }

    if (selectedDay > maxDays) selectedDay = maxDays

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "انتخاب تاریخ شمسی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("روز:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedDay > 1) selectedDay-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = PersianTextNormalizer.toPersianDigits(selectedDay.toString()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(onClick = { if (selectedDay < maxDays) selectedDay++ }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }

                // Month Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ماه:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedMonth > 1) selectedMonth-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = PersianCalendarHelper.MONTH_NAMES.getOrElse(selectedMonth - 1) { "" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(onClick = { if (selectedMonth < 12) selectedMonth++ }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }

                // Year Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("سال:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (selectedYear > 1402) selectedYear-- }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = PersianTextNormalizer.toPersianDigits(selectedYear.toString()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(onClick = { if (selectedYear < 1410) selectedYear++ }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                }

                // Preview
                val computedJdn = remember(selectedYear, selectedMonth, selectedDay) {
                    PersianCalendarHelper.jalaliToJdn(selectedYear, selectedMonth, selectedDay)
                }
                val previewDayName = PersianCalendarHelper.getDayOfWeekName(PersianCalendarHelper.getDayOfWeekIndex(computedJdn))
                Text(
                    text = "$previewDayName، ${PersianTextNormalizer.toPersianDigits(selectedDay.toString())} ${PersianCalendarHelper.MONTH_NAMES.getOrElse(selectedMonth - 1) { "" }} ${PersianTextNormalizer.toPersianDigits(selectedYear.toString())}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jdn = PersianCalendarHelper.jalaliToJdn(selectedYear, selectedMonth, selectedDay)
                    onDateSelected(jdn)
                }
            ) {
                Text("انتخاب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
