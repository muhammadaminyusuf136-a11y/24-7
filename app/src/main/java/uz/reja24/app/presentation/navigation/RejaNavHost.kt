package uz.reja24.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.reja24.app.R
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.presentation.components.RejaBackground
import uz.reja24.app.presentation.home.HomeScreen
import uz.reja24.app.presentation.onboarding.OnboardingScreen
import uz.reja24.app.presentation.prayer.PrayerSettingsScreen
import uz.reja24.app.presentation.muhosaba.MuhosabaEveningScreen
import uz.reja24.app.presentation.muhosaba.MuhosabaScreen
import uz.reja24.app.presentation.schedule.EntryEditorScreen
import uz.reja24.app.presentation.schedule.ScheduleScreen
import uz.reja24.app.presentation.settings.AllowedAppsScreen
import uz.reja24.app.presentation.settings.AppLimitsScreen
import uz.reja24.app.presentation.settings.AppPickerScreen
import uz.reja24.app.presentation.settings.PrivacyPolicyScreen
import uz.reja24.app.presentation.settings.ReliabilityScreen
import uz.reja24.app.presentation.settings.SettingsScreen
import uz.reja24.app.presentation.theme.RejaTheme

private data class TopLevelItem(val route: Route, @StringRes val labelRes: Int, val icon: ImageVector)

private val TOP_LEVEL_ITEMS = listOf(
    TopLevelItem(Route.Today, R.string.nav_today, Icons.Outlined.Today),
    TopLevelItem(Route.Plan, R.string.nav_plan, Icons.Outlined.CalendarMonth),
    TopLevelItem(Route.Results, R.string.nav_results, Icons.Outlined.Checklist),
    TopLevelItem(Route.Settings, R.string.nav_settings, Icons.Outlined.Settings)
)

private const val TRANSITION_MS = 250

@Composable
fun RejaNavHost(
    startWithOnboarding: Boolean,
    backgroundStyle: BackgroundStyle,
    /** Har oshganda kechki muhosaba oynasi ochiladi (eslatmadan kelganda). */
    muhosabaRequest: Int = 0
) {
    val navController = rememberNavController()
    LaunchedEffect(muhosabaRequest) {
        if (muhosabaRequest > 0 && !startWithOnboarding) {
            navController.navigate(Route.MuhosabaEvening) { launchSingleTop = true }
        }
    }
    // Boshlang'ich ekran bir marta tanlanadi (tanishuv tugagach navigatsiya o'zi o'tkazadi)
    val startDestination: Route = remember { if (startWithOnboarding) Route.Onboarding else Route.Today }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TOP_LEVEL_ITEMS.any { item ->
        destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    RejaBackground(style = backgroundStyle) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    RejaBottomBar(
                        isSelected = { item -> destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true },
                        onSelect = { item -> navController.navigateTopLevel(item.route) }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                enterTransition = { fadeIn(tween(TRANSITION_MS)) },
                exitTransition = { fadeOut(tween(TRANSITION_MS)) }
            ) {
                composable<Route.Onboarding> {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Route.Today) {
                                popUpTo(Route.Onboarding) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Route.Today> {
                    HomeScreen(
                        onOpenReliability = { navController.navigate(Route.Reliability) },
                        onOpenPlan = { navController.navigateTopLevel(Route.Plan) },
                        onOpenPrayer = { navController.navigate(Route.PrayerSettings) },
                        onOpenMuhosaba = { navController.navigateTopLevel(Route.Results) }
                    )
                }
                composable<Route.Plan> {
                    ScheduleScreen(
                        onAddEntry = { date -> navController.navigate(Route.EntryEditor(dateEpochDay = date.toEpochDay())) },
                        onEditEntry = { id -> navController.navigate(Route.EntryEditor(entryId = id)) }
                    )
                }
                composable<Route.Results> { MuhosabaScreen() }
                composable<Route.MuhosabaEvening> {
                    MuhosabaEveningScreen(
                        onStart = {
                            navController.popBackStack()
                            navController.navigateTopLevel(Route.Results)
                        },
                        onLater = { navController.popBackStack() }
                    )
                }
                composable<Route.Settings> {
                    SettingsScreen(
                        onOpenAppPicker = { navController.navigate(Route.AppPicker) },
                        onOpenReliability = { navController.navigate(Route.Reliability) },
                        onOpenPrivacy = { navController.navigate(Route.Privacy) },
                        onOpenPrayer = { navController.navigate(Route.PrayerSettings) },
                        onOpenAllowedApps = { navController.navigate(Route.AllowedApps) },
                        onOpenAppLimits = { navController.navigate(Route.AppLimits) }
                    )
                }
                composable<Route.EntryEditor> { EntryEditorScreen(onBack = { navController.popBackStack() }) }
                composable<Route.AppPicker> { AppPickerScreen(onBack = { navController.popBackStack() }) }
                composable<Route.Reliability> { ReliabilityScreen(onBack = { navController.popBackStack() }) }
                composable<Route.Privacy> { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
                composable<Route.AllowedApps> { AllowedAppsScreen(onBack = { navController.popBackStack() }) }
                composable<Route.AppLimits> { AppLimitsScreen(onBack = { navController.popBackStack() }) }
                composable<Route.PrayerSettings> { PrayerSettingsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}

/** Pastki menyu bo'limlari orasida o'tish: har bir bo'lim o'z holatini saqlaydi. */
private fun NavHostController.navigateTopLevel(route: Route) {
    navigate(route) {
        popUpTo(Route.Today) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RejaBottomBar(
    isSelected: (TopLevelItem) -> Boolean,
    onSelect: (TopLevelItem) -> Unit
) {
    val colors = RejaTheme.colors
    NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
        TOP_LEVEL_ITEMS.forEach { item ->
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = isSelected(item),
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentText,
                    selectedTextColor = colors.textPrimary,
                    indicatorColor = colors.accent.copy(alpha = 0.16f),
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted
                )
            )
        }
    }
}

