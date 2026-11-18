package uz.reja24.app.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.reja24.app.core.util.SystemTimeProvider
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.data.local.datastore.AppStateRepositoryImpl
import uz.reja24.app.data.local.datastore.PrayerSettingsRepositoryImpl
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.data.local.datastore.UserPreferencesRepositoryImpl
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.ReminderScheduler
import uz.reja24.app.service.alarm.AlarmScheduler
import uz.reja24.app.data.repository.BlockedAppRepositoryImpl
import uz.reja24.app.data.repository.GoalRepositoryImpl
import uz.reja24.app.data.repository.MuhosabaRepositoryImpl
import uz.reja24.app.domain.repository.MuhosabaRepository
import uz.reja24.app.data.repository.AppRuleRepositoryImpl
import uz.reja24.app.domain.repository.AppRuleRepository
import uz.reja24.app.data.repository.RoutineSettingsRepositoryImpl
import uz.reja24.app.data.repository.ScheduleRepositoryImpl
import uz.reja24.app.data.repository.ScoreRepositoryImpl
import uz.reja24.app.data.repository.StreakRepositoryImpl
import uz.reja24.app.domain.repository.BlockedAppRepository
import uz.reja24.app.domain.repository.GoalRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.ScoreRepository
import uz.reja24.app.domain.repository.StreakRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository

/** "Interfeys so'ralganda — shu implementatsiyani ber" degan bog'lashlar. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds abstract fun bindRoutineSettingsRepository(impl: RoutineSettingsRepositoryImpl): RoutineSettingsRepository
    @Binds abstract fun bindBlockedAppRepository(impl: BlockedAppRepositoryImpl): BlockedAppRepository
    @Binds abstract fun bindScoreRepository(impl: ScoreRepositoryImpl): ScoreRepository
    @Binds abstract fun bindStreakRepository(impl: StreakRepositoryImpl): StreakRepository
    @Binds abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
    @Binds abstract fun bindMuhosabaRepository(impl: MuhosabaRepositoryImpl): MuhosabaRepository
    @Binds abstract fun bindAppRuleRepository(impl: AppRuleRepositoryImpl): AppRuleRepository
    @Binds abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
    @Binds abstract fun bindAppStateRepository(impl: AppStateRepositoryImpl): AppStateRepository
    @Binds abstract fun bindPrayerSettingsRepository(impl: PrayerSettingsRepositoryImpl): PrayerSettingsRepository
    @Binds abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun bindReminderScheduler(impl: AlarmScheduler): ReminderScheduler
}
