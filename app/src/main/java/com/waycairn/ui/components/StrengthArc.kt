package com.waycairn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Strength tiers per BUILD_PLAN.md §4: 0–6 Forming, 7–13 Strengthening, 14–20 Establishing, 21+ Built. */
enum class StrengthTier(val label: String, val floor: Int, val nextAt: Int?) {
    FORMING("Forming", 0, 7),
    STRENGTHENING("Strengthening", 7, 14),
    ESTABLISHING("Establishing", 14, 21),
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

@Composable
fun StrengthArc(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val tier = strengthTierFor(streak)
    val target = tierProgress(streak)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 250),
        label = "strengthProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = tier.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val trailing = tier.nextAt?.let { "$streak → $it days" } ?: "$streak days · milestone reached"
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (tier == StrengthTier.BUILT) {
                "Built — 21-day milestone reached."
            } else {
                "The 21-day line marks \"Built\"."
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
