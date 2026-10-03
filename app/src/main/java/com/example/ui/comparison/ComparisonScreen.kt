package com.example.ui.comparison

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.DiffType
import com.example.domain.model.ScheduleClassDiff
import com.example.domain.model.ScheduleComparisonSummary
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.theme.DiffAdded
import com.example.ui.theme.DiffModified
import com.example.ui.theme.DiffRemoved
import com.example.ui.theme.DiffUnchanged

@Composable
fun ComparisonScreen(
    summary: ScheduleComparisonSummary?,
    onRefreshComparison: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        onRefreshComparison()
    }

    var selectedFilter by remember { mutableStateOf<DiffType?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_from_comparison")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "تغییرات برنامه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "مقایسه برنامه انتخابی شما با آخرین نسخه دانشگاه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (summary == null || summary.diffs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "اطلاعات مقایسه‌ای در دسترس نیست.\nابتدا برنامه دانشگاه را دریافت فرمایید.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRefreshComparison,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("refresh_comparison_button")
                    ) {
                        Text("بررسی مجدد")
                    }
                }
            }
        } else {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    count = summary.modifiedCount,
                    label = "تغییر یافته",
                    color = DiffModified,
                    isSelected = selectedFilter == DiffType.MODIFIED,
                    onClick = { selectedFilter = if (selectedFilter == DiffType.MODIFIED) null else DiffType.MODIFIED },
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    count = summary.unchangedCount,
                    label = "بدون تغییر",
                    color = DiffUnchanged,
                    isSelected = selectedFilter == DiffType.UNCHANGED,
                    onClick = { selectedFilter = if (selectedFilter == DiffType.UNCHANGED) null else DiffType.UNCHANGED },
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    count = summary.addedCount,
                    label = "جدید",
                    color = DiffAdded,
                    isSelected = selectedFilter == DiffType.ADDED,
                    onClick = { selectedFilter = if (selectedFilter == DiffType.ADDED) null else DiffType.ADDED },
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    count = summary.removedCount,
                    label = "حذف شده",
                    color = DiffRemoved,
                    isSelected = selectedFilter == DiffType.REMOVED,
                    onClick = { selectedFilter = if (selectedFilter == DiffType.REMOVED) null else DiffType.REMOVED },
                    modifier = Modifier.weight(1f)
                )
            }

            val filteredDiffs = if (selectedFilter == null) {
                summary.diffs
            } else {
                summary.diffs.filter { it.diffType == selectedFilter }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    filteredDiffs,
                    key = { index, diff -> "${index}_${diff.courseName}_${diff.groupCode}_${diff.diffType.name}" },
                    contentType = { _, diff -> diff.diffType.name }
                ) { _, diffItem ->
                    DiffCard(diff = diffItem, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    count: Int,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("stat_badge_$label"),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) color.copy(alpha = 0.9f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = PersianTextNormalizer.toPersianDigits(count.toString()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DiffCard(
    diff: ScheduleClassDiff,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeColor, icon) = when (diff.diffType) {
        DiffType.UNCHANGED -> Triple("✓ بدون تغییر", DiffUnchanged, Icons.Default.CheckCircle)
        DiffType.MODIFIED -> Triple("⚠ تغییر یافته", DiffModified, Icons.Default.Edit)
        DiffType.ADDED -> Triple("+ کلاس جدید در دانشگاه", DiffAdded, Icons.Default.AddCircleOutline)
        DiffType.REMOVED -> Triple("- حذف شده از برنامه", DiffRemoved, Icons.Default.RemoveCircleOutline)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("diff_card_${diff.courseName}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Course Name + Diff Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diff.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (diff.diffType) {
                DiffType.UNCHANGED -> {
                    val c = diff.universityClass ?: diff.currentClass
                    if (c != null) {
                        Text(
                            text = "${c.dayOfWeek} ${PersianTextNormalizer.toPersianDigits(c.startTime)} تا ${PersianTextNormalizer.toPersianDigits(c.endTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استاد: ${c.teacher.ifEmpty { "مشخص نشده" }}  |  کلاس: ${PersianTextNormalizer.toPersianDigits(c.classroom)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                DiffType.MODIFIED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        diff.changes.forEach { change ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${change.fieldName}: ",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${PersianTextNormalizer.toPersianDigits(change.oldValue)} ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = " → ",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DiffModified
                                    )
                                    Text(
                                        text = " ${PersianTextNormalizer.toPersianDigits(change.newValue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DiffModified
                                    )
                                }
                            }
                        }
                    }
                }

                DiffType.ADDED -> {
                    val c = diff.universityClass
                    if (c != null) {
                        Text(
                            text = "زمان: ${c.dayOfWeek} ${PersianTextNormalizer.toPersianDigits(c.startTime)} الی ${PersianTextNormalizer.toPersianDigits(c.endTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DiffAdded
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استاد: ${c.teacher}  |  کلاس: ${PersianTextNormalizer.toPersianDigits(c.classroom)}  |  گروه: ${PersianTextNormalizer.toPersianDigits(c.groupCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DiffType.REMOVED -> {
                    val c = diff.currentClass
                    if (c != null) {
                        Text(
                            text = "برنامه قبلی: ${c.dayOfWeek} ${PersianTextNormalizer.toPersianDigits(c.startTime)} الی ${PersianTextNormalizer.toPersianDigits(c.endTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DiffRemoved
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استاد قبلی: ${c.teacher}  |  کلاس: ${PersianTextNormalizer.toPersianDigits(c.classroom)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
