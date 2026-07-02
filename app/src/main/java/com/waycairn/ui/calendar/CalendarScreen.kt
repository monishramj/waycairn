package com.waycairn.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import java.time.YearMonth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waycairn.ui.components.StreakBanner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val monthLabel = state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val maxCount = (state.countsByDay.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val currentYearMonth = YearMonth.now()
    val canGoForward = state.month.isBefore(currentYearMonth)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Streak section
            Text(
                text = "Streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            StreakBanner(
                streak = state.streak,
                level = state.streakLevel
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = viewModel::previousMonth) { Text("‹") }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = viewModel::nextMonth,
                    enabled = canGoForward
                ) {
                    Text(
                        "›",
                        color = if (canGoForward) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            WeekdayHeader()
            Spacer(Modifier.height(4.dp))
            MonthGrid(
                month = state.month,
                counts = state.countsByDay,
                maxCount = maxCount,
                today = LocalDate.now(),
                onDayClick = viewModel::selectDay
            )
        }
    }

    val selection = state.selected
    if (selection != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSelection,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = selection.date.format(
                        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                if (selection.completed.isEmpty() && selection.missed.isEmpty()) {
                    Text(
                        "Nothing recorded this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selection.completed.isNotEmpty()) {
                    DaySheetSection(
                        title = "Completed",
                        habits = selection.completed,
                        marker = "◆",
                        markerColor = MaterialTheme.colorScheme.primary
                    )
                }

                if (selection.missed.isNotEmpty()) {
                    if (selection.completed.isNotEmpty()) Spacer(Modifier.height(16.dp))
                    DaySheetSection(
                        title = "Missed",
                        habits = selection.missed,
                        marker = "◇",
                        markerColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DaySheetSection(
    title: String,
    habits: List<com.waycairn.data.model.Habit>,
    marker: String,
    markerColor: Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
    habits.forEach { habit ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = marker,
                style = MaterialTheme.typography.bodyLarge,
                color = markerColor,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = habit.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Monday-first week
        val days = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        days.forEach { d ->
            Text(
                text = d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: java.time.YearMonth,
    counts: Map<LocalDate, Int>,
    maxCount: Int,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()
    val cells = leadingBlanks + daysInMonth
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                count = counts[date] ?: 0,
                                maxCount = maxCount,
                                isToday = date == today,
                                isFuture = date.isAfter(today),
                                onClick = { onDayClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    maxCount: Int,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val ring = MaterialTheme.colorScheme.primary
    val futureColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val targetFill = when {
        isFuture -> futureColor
        count == 0 -> base
        else -> {
            val steps = 4
            val raw = (count.toFloat() / maxCount).coerceIn(0f, 1f)
            val stepped = (kotlin.math.ceil(raw * steps) / steps).coerceIn(0.3f, 1f)
            lerp(base, accent, stepped)
        }
    }
    val fill by animateColorAsState(
        targetValue = targetFill,
        animationSpec = tween(durationMillis = 200),
        label = "dayFill"
    )

    // Circle shape — aspectRatio(1f) on the parent Box guarantees it's square.
    val shape = androidx.compose.foundation.shape.CircleShape
    val ringModifier = if (isToday) {
        Modifier.border(BorderStroke(2.dp, ring), shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(fill)
            .then(ringModifier)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // No day number shown — just a coloured circle.
    }
}

private fun lerp(start: Color, stop: Color, fraction: Float): Color = Color(
    red = start.red + (stop.red - start.red) * fraction,
    green = start.green + (stop.green - start.green) * fraction,
    blue = start.blue + (stop.blue - start.blue) * fraction,
    alpha = start.alpha + (stop.alpha - start.alpha) * fraction
)
