package com.waycairn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waycairn.data.prefs.ThemeMode
import com.waycairn.ui.apps.AppPickerScreen
import com.waycairn.ui.calendar.CalendarScreen
import com.waycairn.ui.habits.HabitDetailScreen
import com.waycairn.ui.habits.HabitEditScreen
import com.waycairn.ui.habits.HabitListScreen
import com.waycairn.ui.nav.Routes
import com.waycairn.ui.onboarding.OnboardingScreen
import com.waycairn.ui.settings.SettingsScreen
import com.waycairn.ui.theme.WaycairnTheme

class MainActivity : ComponentActivity() {

    // Habit id to deep-link to (from an overlay row tap), consumed once by the NavHost.
    private var pendingHabitId by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingHabitId = habitIdFromIntent(intent)
        requestNotificationPermissionIfNeeded()
        val settingsStore = (application as WaycairnApp).settingsStore
        setContent {
            val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // null = not yet loaded from DataStore; avoids flashing the wrong start screen.
            val onboardingComplete by produceState<Boolean?>(initialValue = null) {
                settingsStore.onboardingComplete.collect { value = it }
            }
            WaycairnTheme(darkTheme = darkTheme) {
                WaycairnRoot(
                    pendingHabitId = pendingHabitId,
                    onHabitConsumed = { pendingHabitId = null },
                    onboardingComplete = onboardingComplete
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        habitIdFromIntent(intent)?.let { pendingHabitId = it }
    }

    private fun habitIdFromIntent(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_HABIT_ID, -1L) ?: -1L
        return if (id > 0) id else null
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}

private data class BottomDestination(val route: String, val label: String, val glyph: String)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HABITS, "Habits", "◈"),
    BottomDestination(Routes.CALENDAR, "Calendar", "▦"),
    BottomDestination(Routes.APPS, "Apps", "◱"),
    BottomDestination(Routes.SETTINGS, "Settings", "⚙")
)

@Composable
fun WaycairnRoot(
    pendingHabitId: Long? = null,
    onHabitConsumed: () -> Unit = {},
    onboardingComplete: Boolean? = true
) {
    // Not yet loaded from DataStore — render nothing rather than flash the wrong start screen.
    if (onboardingComplete == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomNav

    LaunchedEffect(pendingHabitId) {
        pendingHabitId?.let { id ->
            navController.navigate(Routes.habitDetail(id))
            onHabitConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(Routes.HABITS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Text(dest.glyph, style = MaterialTheme.typography.titleMedium) },
                            label = { Text(dest.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingComplete) Routes.HABITS else Routes.ONBOARDING,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.HABITS) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HABITS) {
                HabitListScreen(
                    onAddHabit = { navController.navigate(Routes.habitEdit()) },
                    onOpenHabit = { id -> navController.navigate(Routes.habitDetail(id)) }
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.APPS) { AppPickerScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(
                route = Routes.HABIT_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_HABIT_ID) { type = NavType.LongType })
            ) {
                HabitDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.habitEdit(id)) }
                )
            }
            composable(
                route = Routes.HABIT_EDIT,
                arguments = listOf(
                    navArgument(Routes.ARG_HABIT_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                HabitEditScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
