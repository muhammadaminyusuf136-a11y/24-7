package uz.reja24.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uz.reja24.app.presentation.MainViewModel
import uz.reja24.app.presentation.navigation.RejaNavHost
import uz.reja24.app.presentation.theme.RejaTheme
import uz.reja24.app.presentation.theme.resolveRejaColors
import uz.reja24.app.service.alarm.NotificationHelper

/**
 * Yagona asosiy Activity — barcha ekranlar Compose orqali shu yerda chiziladi.
 * AppCompatActivity ishlatilgan, chunki til almashtirish
 * (AppCompatDelegate.setApplicationLocales) Android 12 va pastda faqat shunda ishlaydi.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Kechki muhosaba eslatmasidan ochilganda oshadi — navigatsiya shu oynaga o'tadi. */
    private var muhosabaRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Kanallar nomlari joriy tilda bo'lsin (til almashganda Activity qayta yaratiladi)
        NotificationHelper.createChannels(this)
        if (savedInstanceState == null && intent?.getBooleanExtra(EXTRA_OPEN_MUHOSABA, false) == true) muhosabaRequest++

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val current = state
            if (current != null) {
                val isDark = resolveRejaColors(
                    current.themeMode, current.backgroundStyle, isSystemInDarkTheme()
                ).isDark
                LaunchedEffect(isDark) {
                    // Status bar ikonkalari fon rangiga mos bo'lsin
                    val style = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                RejaTheme(themeMode = current.themeMode, backgroundStyle = current.backgroundStyle) {
                    RejaNavHost(
                        startWithOnboarding = !current.isOnboardingCompleted,
                        backgroundStyle = current.backgroundStyle,
                        muhosabaRequest = muhosabaRequest
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_MUHOSABA, false)) muhosabaRequest++
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    companion object {
        const val EXTRA_OPEN_MUHOSABA = "open_muhosaba"
    }
}
