package uz.reja24.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import uz.reja24.app.data.local.db.DatabaseMigrations
import uz.reja24.app.data.local.db.RejaDatabase
import uz.reja24.app.data.local.db.dao.BlockedAppDao
import uz.reja24.app.data.local.db.dao.DailyScoreDao
import uz.reja24.app.data.local.db.dao.GoalDao
import uz.reja24.app.data.local.db.dao.MuhosabaDao
import uz.reja24.app.data.local.db.dao.RoutineSettingsDao
import uz.reja24.app.data.local.db.dao.ScheduleDao
import uz.reja24.app.data.local.db.dao.StreakDao
import uz.reja24.app.data.local.db.dao.TaskCompletionDao
import uz.reja24.app.data.local.db.dao.WeeklyAwardDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RejaDatabase =
        Room.databaseBuilder(context, RejaDatabase::class.java, RejaDatabase.NAME)
            .addMigrations(*DatabaseMigrations.ALL)
            // Faqat versiya pastga tushganda (sinov paytida) baza tozalanadi.
            // Oddiy yangilanishda ma'lumot HECH QACHON o'chirilmaydi.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides fun provideScheduleDao(db: RejaDatabase): ScheduleDao = db.scheduleDao()
    @Provides fun provideTaskCompletionDao(db: RejaDatabase): TaskCompletionDao = db.taskCompletionDao()
    @Provides fun provideRoutineSettingsDao(db: RejaDatabase): RoutineSettingsDao = db.routineSettingsDao()
    @Provides fun provideBlockedAppDao(db: RejaDatabase): BlockedAppDao = db.blockedAppDao()
    @Provides fun provideDailyScoreDao(db: RejaDatabase): DailyScoreDao = db.dailyScoreDao()
    @Provides fun provideWeeklyAwardDao(db: RejaDatabase): WeeklyAwardDao = db.weeklyAwardDao()
    @Provides fun provideStreakDao(db: RejaDatabase): StreakDao = db.streakDao()
    @Provides fun provideGoalDao(db: RejaDatabase): GoalDao = db.goalDao()
    @Provides fun provideMuhosabaDao(db: RejaDatabase): MuhosabaDao = db.muhosabaDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // Fayl buzilgan bo'lsa — bo'sh sozlamalar bilan qayta yaratiladi, ilova yiqilmaydi
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
        produceFile = { context.preferencesDataStoreFile("reja24_prefs") }
    )
}
