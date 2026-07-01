package com.waycairn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Home banner: a small cairn (stack of stones) that grows with the global streak, plus the count.
 * The stone stack tops out at 5 stones so the motif stays calm; the number carries the exact value.
 */
@Composable
fun StreakBanner(
    globalStreak: Int,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentSoft = MaterialTheme.colorScheme.secondary
    val stones = (1 + globalStreak.coerceAtMost(20) / 4).coerceIn(1, 5)

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
            CairnGlyph(
                stoneCount = stones,
                base = accent,
                soft = accentSoft,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = if (globalStreak == 1) "1 day" else "$globalStreak days",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (globalStreak == 0) {
                        "Stack your first perfect day"
                    } else {
                        "Perfect-day streak"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CairnGlyph(
    stoneCount: Int,
    base: Color,
    soft: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = stoneCount.coerceIn(1, 5)
        val gap = size.height * 0.06f
        val stoneH = (size.height - gap * (count - 1)) / count
        // Widest stone at the bottom, narrowing toward the top.
        for (i in 0 until count) {
            val fromBottom = i // 0 == bottom
            val widthFactor = 1f - fromBottom * 0.14f
            val stoneW = size.width * widthFactor
            val left = (size.width - stoneW) / 2f
            val top = size.height - (fromBottom + 1) * stoneH - fromBottom * gap
            drawRoundRect(
                color = if (fromBottom % 2 == 0) base else soft,
                topLeft = Offset(left, top),
                size = Size(stoneW, stoneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(stoneH / 2f, stoneH / 2f)
            )
        }
    }
}
