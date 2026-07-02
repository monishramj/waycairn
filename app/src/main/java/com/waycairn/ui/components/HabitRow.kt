package com.waycairn.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.waycairn.data.model.Habit

/**
 * A single habit line: leading round check control (toggles completion), title + deadline.
 * Tapping the check plays a ripple ring + animates the strike-through into place.
 * Tapping the row body (not the check) opens detail.
 */
@Composable
fun HabitRow(
    habit: Habit,
    doneToday: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Strike-through progress: 0 = no line, 1 = fully drawn across the title.
    val strikeTarget = if (doneToday) 1f else 0f
    val strikeProgress by animateFloatAsState(
        targetValue = strikeTarget,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "strike"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckStone(done = doneToday, onToggle = onToggle)

            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                val strikeColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (doneToday) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    // Base TextDecoration is null; we draw the animated line manually below.
                    textDecoration = TextDecoration.None,
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        if (strikeProgress > 0f) {
                            val y = size.height / 2f
                            drawLine(
                                color = strikeColor,
                                start = Offset(0f, y),
                                end = Offset(size.width * strikeProgress, y),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                )
                Text(
                    text = deadlineLabel(habit.deadlineMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckStone(
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ring = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primary

    // Ripple: scale 0→1.6 then back to 0, alpha peaks at 0.35.
    val rippleScale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    var triggerRipple by remember { mutableStateOf(false) }

    LaunchedEffect(triggerRipple) {
        if (!triggerRipple) return@LaunchedEffect
        // Run scale and alpha in parallel.
        rippleAlpha.snapTo(0.35f)
        rippleScale.snapTo(0f)
        rippleScale.animateTo(
            1.6f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
        rippleAlpha.animateTo(0f, animationSpec = tween(durationMillis = 160))
        rippleScale.snapTo(0f)
    }

    Box(
        modifier = modifier.size(40.dp), // extra room for the ripple ring to expand into
        contentAlignment = Alignment.Center
    ) {
        // Ripple ring drawn behind the stone.
        val rippleColor = fill
        if (rippleScale.value > 0f) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            color = rippleColor.copy(alpha = rippleAlpha.value),
                            radius = (size.minDimension / 2f) * rippleScale.value,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )
        }

        // The stone itself.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(fill)
                    else Modifier.border(BorderStroke(1.5.dp, ring), CircleShape)
                )
                .clickable {
                    triggerRipple = !triggerRipple
                    onToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
