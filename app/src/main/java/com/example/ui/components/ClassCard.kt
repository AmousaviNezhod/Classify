package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.theme.ParityEvenColor
import com.example.ui.theme.ParityOddColor
import com.example.ui.theme.tactileClick

/**
 * One class offering. The card carries three levels of information and they are
 * separated by weight and tone rather than by boxes:
 *
 *   1. identity     — course name (title, highest contrast)
 *   2. when/where   — time block on a raised strip, room + teacher as quiet meta
 *   3. classification — code, units, group, week parity as small chips
 *
 * A 3dp rail on the leading edge marks the card as a schedule block; its gradient
 * is the only place in the component that uses more than one tone.
 */
@Composable
fun ClassCard(
    scheduleClass: ScheduleClass,
    modifier: Modifier = Modifier,
    missingRoomText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.large
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("class_card_${scheduleClass.id}")
            .then(if (onClick != null) Modifier.tactileClick(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
    ) {
        // The rail is drawn behind the content instead of being laid out as a
        // sibling: drawBehind always spans the true measured height, which a
        // fillMaxHeight child cannot guarantee inside an unbounded list item.
        // Drawn on the physical right edge — this app is RTL-locked.
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val railWidth = 3.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.08f)),
                            startY = 0f,
                            endY = size.height
                        ),
                        topLeft = Offset(size.width - railWidth, 0f),
                        size = Size(railWidth, size.height)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 17.dp, top = 14.dp, bottom = 14.dp)
            ) {
                // ------------------------------------------------ classification chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scheduleClass.courseCode.isNotBlank()) {
                            MetaChip("کد ${PersianTextNormalizer.toPersianDigits(scheduleClass.courseCode)}")
                        }
                        if (scheduleClass.units > 0) {
                            MetaChip("${PersianTextNormalizer.toPersianDigits(scheduleClass.units.toString())} واحد")
                        }
                        val parityText = when {
                            scheduleClass.parity.contains("زوج") -> "هفته زوج"
                            scheduleClass.parity.contains("فرد") -> "هفته فرد"
                            else -> "تمام هفته‌ها"
                        }
                        val isEven = scheduleClass.parity.contains("زوج")
                        ParityChip(
                            text = parityText,
                            color = when {
                                isEven -> ParityEvenColor
                                scheduleClass.parity.contains("فرد") -> ParityOddColor
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            filled = isEven
                        )
                    }

                    if (scheduleClass.groupCode.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = accent.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = "گروه ${PersianTextNormalizer.toPersianDigits(scheduleClass.groupCode)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---------------------------------------------------------------- identity
                Text(
                    text = scheduleClass.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(11.dp))

                // ------------------------------------------------------------ time strip
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "زمان",
                            modifier = Modifier.size(15.dp),
                            tint = muted
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = PersianTextNormalizer.toPersianDigits(scheduleClass.startTime),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " – ${PersianTextNormalizer.toPersianDigits(scheduleClass.endTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = scheduleClass.dayOfWeek,
                            style = MaterialTheme.typography.labelMedium,
                            color = muted
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ------------------------------------------------------------------- meta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (scheduleClass.teacher.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "استاد",
                                modifier = Modifier.size(15.dp),
                                tint = muted
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = scheduleClass.teacher,
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val room = scheduleClass.classroom.ifBlank { missingRoomText ?: "" }
                    if (room.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = "کلاس",
                                modifier = Modifier.size(15.dp),
                                tint = if (scheduleClass.classroom.isBlank()) {
                                    MaterialTheme.colorScheme.outline
                                } else muted
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = PersianTextNormalizer.toPersianDigits(room),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (scheduleClass.classroom.isBlank()) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                },
                                maxLines = 1
                            )
                        }
                    }
                }

                if (scheduleClass.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = scheduleClass.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ParityChip(text: String, color: Color, filled: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = if (filled) color.copy(alpha = 0.16f) else Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = if (filled) 0.34f else 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
