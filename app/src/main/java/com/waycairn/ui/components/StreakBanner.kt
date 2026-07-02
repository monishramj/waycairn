package com.waycairn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Today's streak state: nothing done, a dim day (>=1 habit), or a full day (all habits). */
enum class StreakLevel { NONE, PARTIAL, FULL }

/**
 * Streak banner for the Calendar tab.
 *
 * The fire tracks the kept-alive streak (>=1 habit/day). It's unlit when nothing is done today,
 * a dim ember once at least one habit is done, and a full flame — with a ripple ring — once every
 * habit is done. [streak] is the number of consecutive kept-alive days.
 */
@Composable
fun StreakBanner(
    streak: Int,
    level: StreakLevel,
    modifier: Modifier = Modifier
) {
    val litColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dimColor = litColor.copy(alpha = 0.55f)

    val targetColor = when (level) {
        StreakLevel.FULL -> litColor
        StreakLevel.PARTIAL -> dimColor
        StreakLevel.NONE -> mutedColor
    }
    val iconColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 250),
        label = "fireColor"
    )

    // Ripple: fires once each time the day becomes full.
    val rippleScale = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    var prevFull by remember { mutableStateOf(level == StreakLevel.FULL) }

    LaunchedEffect(level) {
        val isFull = level == StreakLevel.FULL
        if (isFull && !prevFull) {
            rippleAlpha.snapTo(0.5f)
            rippleScale.snapTo(0f)
            rippleScale.animateTo(
                2.2f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
            rippleAlpha.animateTo(0f, animationSpec = tween(durationMillis = 240))
            rippleScale.snapTo(0f)
        }
        prevFull = isFull
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon + ripple ring in a Box so the ring can overdraw.
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val ringColor = litColor
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .drawWithContent {
                            drawContent()
                            if (rippleScale.value > 0f) {
                                drawCircle(
                                    color = ringColor.copy(alpha = rippleAlpha.value),
                                    radius = (size.minDimension / 2f) * rippleScale.value,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        },
                    tint = iconColor
                )
            }

            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = if (streak == 1) "1 day" else "$streak days",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (level) {
                        StreakLevel.FULL -> "Perfect day — streak full"
                        StreakLevel.PARTIAL -> "Streak kept — finish all for a full day"
                        StreakLevel.NONE ->
                            if (streak == 0) "Complete a habit to start a streak"
                            else "Complete a habit to keep your streak"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
