package com.waycairn.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.waycairn.WaycairnApp
import com.waycairn.util.Permissions
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val title: String,
    val description: String,
    val note: String? = null,
    val granted: (android.content.Context) -> Boolean,
    val intent: (android.content.Context) -> android.content.Intent
)

private val steps = listOf(
    OnboardingStep(
        title = "Notifications",
        description = "Lets WayCairn remind you before a timed habit's deadline, and tell you if you missed it.",
        granted = { Permissions.notificationsEnabled(it) },
        intent = { Permissions.notificationSettingsIntent(it) }
    ),
    OnboardingStep(
        title = "Display over other apps",
        description = "Needed so the friction screen can appear on top of an app you've gated.",
        granted = { Permissions.canDrawOverlay(it) },
        intent = { Permissions.overlaySettingsIntent(it) }
    ),
    OnboardingStep(
        title = "Alarms & reminders",
        description = "Keeps reminder and missed-deadline notifications firing at the exact minute.",
        granted = { Permissions.canScheduleExactAlarms(it) },
        intent = { Permissions.exactAlarmSettingsIntent(it) }
    ),
    OnboardingStep(
        title = "Accessibility",
        description = "Detects when you open a gated app — this is what makes the friction screen show up at all.",
        note = "Sideloaded on Android 13+: the toggle may show as \"Restricted setting\" the first time. " +
            "Fix it via Settings → Apps → WayCairn → ⋮ menu (top right) → \"Allow restricted settings\" → " +
            "confirm, THEN enable it under Settings → Accessibility → Installed services → WayCairn.",
        granted = { Permissions.accessibilityEnabled(it) },
        intent = { Permissions.accessibilitySettingsIntent() }
    )
)

/**
 * First-run walkthrough of the 4 interceptor permissions, in order, each with a live status check
 * and a deep-link button. Nothing here is a hard gate — WayCairn is a fully usable habit tracker
 * without any of these; they're only required for the interceptor (overlay + notifications) to work.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Bump on ON_RESUME so statuses re-check after returning from a permission settings screen.
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Set up WayCairn",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Four permissions power the interceptor — the overlay that appears when you " +
                    "open an app you've gated. WayCairn works as a plain habit tracker without any " +
                    "of them; you can grant these now or later from Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            steps.forEachIndexed { index, step ->
                val granted = remember(refreshKey) { step.granted(context) }
                OnboardingStepCard(
                    index = index + 1,
                    title = step.title,
                    description = step.description,
                    note = step.note,
                    granted = granted,
                    onOpen = { context.startActivity(step.intent(context)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val app = context.applicationContext as WaycairnApp
                    scope.launch {
                        app.settingsStore.setOnboardingComplete(true)
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to WayCairn")
            }

            TextButton(
                onClick = {
                    val app = context.applicationContext as WaycairnApp
                    scope.launch {
                        app.settingsStore.setOnboardingComplete(true)
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Skip for now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingStepCard(
    index: Int,
    title: String,
    description: String,
    note: String?,
    granted: Boolean,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepBadge(index = index, granted = granted)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (granted) "Granted" else "Not granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (granted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (granted) "Open settings" else "Grant permission")
            }
        }
    }
}

@Composable
private fun StepBadge(index: Int, granted: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (granted) "✓" else index.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
