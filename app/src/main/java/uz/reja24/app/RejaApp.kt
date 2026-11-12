package uz.reja24.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.reja24.app.core.di.ApplicationScope
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.service.alarm.NotificationHelper
import uz.reja24.app.service.blocking.ScreenEventTracker
import javax.inject.Inject

/**
 * Ilovaning "yuragi". Ilova ishga tushganda birinchi bo'lib shu klass yaratiladi.
 * @HiltAndroidApp — Hilt (bog'liqliklarni avtomatik ulash tizimi) shu yerdan boshlanadi.
 * Configuration.Provider — WorkManager Hilt orqali ishchilarni (Worker) yaratishi uchun.
 */
@HiltAndroidApp
class RejaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var screenEventTracker: ScreenEventTracker
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        screenEventTracker.register()
        applicationScope.launch {
            // Bildirishnoma kanallari foydalanuvchi tanlagan tilda yaratiladi
            val prefs = runCatching { userPreferencesRepository.preferences.first() }.getOrNull()
            val language = prefs?.takeIf { it.isLanguageChosen }?.language
            NotificationHelper.createChannels(LocaleHelper.localizedContext(this@RejaApp, language))
        }
    }
}
