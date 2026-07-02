package com.waycairn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Strength tiers: 0–6 Begin, 7–13 Form, 14–20 Harden, 21+ Built. */
enum class StrengthTier(val label: String, val floor: Int, val nextAt: Int?) {
    FORMING("Begin", 0, 7),
    STRENGTHENING("Form", 7, 14),
    ESTABLISHING("Harden", 14, 21),
    BUILT("Built", 21, null)
}

fun strengthTierFor(streak: Int): StrengthTier = when {
    streak >= 21 -> StrengthTier.BUILT
    streak >= 14 -> StrengthTier.ESTABLISHING
    streak >= 7 -> StrengthTier.STRENGTHENING
    else -> StrengthTier.FORMING
}

/** Fraction (0f..1f) of progress from the current tier's floor toward its next threshold. */
fun tierProgress(streak: Int): Float {
    val tier = strengthTierFor(streak)
    val next = tier.nextAt ?: return 1f
    return ((streak - tier.floor).toFloat() / (next - tier.floor)).coerceIn(0f, 1f)
}

private const val BUILT_DAYS = 21

/**
 * A calm progress indicator spanning 0 → 21 days. The four tiers are labeled beneath their
 * segments, the 21-day "Built" milestone is marked at the end, and both the current and best
 * streaks are shown (best as a subtle notch above the track). Motion is a short fade only.
 */
@Composable
fun StrengthArc(
    streak: Int,
    bestStreak: Int = streak,
    modifier: Modifier = Modifier
) {
    val tier = strengthTierFor(streak)

    val currentFraction = (streak.toFloat() / BUILT_DAYS).coerceIn(0f, 1f)
    val bestFraction = (bestStreak.toFloat() / BUILT_DAYS).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = currentFraction,
        animationSpec = tween(durationMillis = 250),
        label = "strengthFill"
    )

    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val divider = MaterialTheme.colorScheme.outline
    val bestColor = MaterialTheme.colorScheme.secondary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Current + best streak headline.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (streak == 1) "Current · 1 day" else "Current · $streak days",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (bestStreak == 1) "Best · 1 day" else "Best · $bestStreak days",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val trackH = 10.dp.toPx()
            val trackY = size.height - trackH
            val radius = CornerRadius(trackH / 2f, trackH / 2f)

            // Base track.
            drawRoundRect(
                color = track,
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackH),
                cornerRadius = radius
            )

            // Filled portion up to current streak.
            if (animatedFill > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, trackY),
                    size = Size(size.width * animatedFill, trackH),
                    cornerRadius = radius
                )
            }

            // Tier dividers at days 7 and 14 (thirds of the 0..21 span).
            listOf(1f / 3f, 2f / 3f).forEach { f ->
                val x = size.width * f
                drawLine(
                    color = divider,
                    start = Offset(x, trackY),
                    end = Offset(x, trackY + trackH),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 21-day "Built" milestone at the far right: filled diamond when reached, hollow before.
            val milestoneCx = size.width - trackH / 2f
            val milestoneCy = trackY + trackH / 2f
            val half = trackH * 0.7f
            val diamond = Path().apply {
                moveTo(milestoneCx, milestoneCy - half)
                lineTo(milestoneCx + half, milestoneCy)
                lineTo(milestoneCx, milestoneCy + half)
                lineTo(milestoneCx - half, milestoneCy)
                close()
            }
            if (streak >= BUILT_DAYS) {
                drawPath(diamond, color = accent)
            } else {
                drawPath(diamond, color = track)
                drawPath(
                    diamond,
                    color = divider,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }

            // Best-streak notch above the track (only when it's ahead of the current fill).
            if (bestStreak > 0 && bestFraction > animatedFill) {
                val bx = (size.width * bestFraction).coerceIn(0f, size.width)
                val notchH = 6.dp.toPx()
                val notch = Path().apply {
                    moveTo(bx, trackY - 2.dp.toPx())
                    lineTo(bx - notchH / 2f, trackY - notchH - 2.dp.toPx())
                    lineTo(bx + notchH / 2f, trackY - notchH - 2.dp.toPx())
                    close()
                }
                drawPath(notch, color = bestColor)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tier labels: Begin / Form / Harden / Built pinned at their segment positions.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TierLabel("Begin", tier == StrengthTier.FORMING, accent, onSurfaceVariant, Modifier.weight(1f), TextAlign.Start)
            TierLabel("Form", tier == StrengthTier.STRENGTHENING, accent, onSurfaceVariant, Modifier.weight(1f), TextAlign.Center)
            TierLabel("Harden", tier == StrengthTier.ESTABLISHING, accent, onSurfaceVariant, Modifier.weight(1f), TextAlign.Center)
            TierLabel("Built · 21", tier == StrengthTier.BUILT, accent, onSurfaceVariant, Modifier.weight(1f), TextAlign.End)
        }
    }
}

@Composable
private fun TierLabel(
    text: String,
    active: Boolean,
    activeColor: Color,
    idleColor: Color,
    modifier: Modifier = Modifier,
    align: TextAlign
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) activeColor else idleColor,
        textAlign = align,
        modifier = modifier
    )
}
