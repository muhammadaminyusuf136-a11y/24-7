package uz.reja24.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import uz.reja24.app.data.local.db.dao.BlockedAppDao
import uz.reja24.app.data.local.db.dao.DailyScoreDao
import uz.reja24.app.data.local.db.dao.GoalDao
import uz.reja24.app.data.local.db.dao.MuhosabaDao
import uz.reja24.app.data.local.db.dao.RoutineSettingsDao
import uz.reja24.app.data.local.db.dao.ScheduleDao
import uz.reja24.app.data.local.db.dao.StreakDao
import uz.reja24.app.data.local.db.dao.TaskCompletionDao
import uz.reja24.app.data.local.db.dao.WeeklyAwardDao
import uz.reja24.app.data.local.db.entity.BlockedAppEntity
import uz.reja24.app.data.local.db.entity.DailyScoreEntity
import uz.reja24.app.data.local.db.entity.GoalEntity
import uz.reja24.app.data.local.db.entity.MuhosabaItemEntity
import uz.reja24.app.data.local.db.entity.MuhosabaMarkEntity
import uz.reja24.app.data.local.db.entity.RoutineSettingsEntity
import uz.reja24.app.data.local.db.entity.ScheduleEntryEntity
import uz.reja24.app.data.local.db.entity.StreakRecordEntity
import uz.reja24.app.data.local.db.entity.TaskCompletionEntity
import uz.reja24.app.data.local.db.entity.WeeklyAwardEntity

/**
 * Ilovaning yagona ma'lumotlar bazasi (faqat telefonda saqlanadi).
 * exportSchema = true — har bir versiya sxemasi app/schemas papkasiga yoziladi,
 * bu migratsiyalarni yozish va tekshirish uchun zarur.
 */
@Database(
    entities = [
        ScheduleEntryEntity::class,
        TaskCompletionEntity::class,
        RoutineSettingsEntity::class,
        BlockedAppEntity::class,
        DailyScoreEntity::class,
        WeeklyAwardEntity::class,
        StreakRecordEntity::class,
        GoalEntity::class,
        MuhosabaItemEntity::class,
        MuhosabaMarkEntity::class
    ],
    version = RejaDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RejaDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun routineSettingsDao(): RoutineSettingsDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun dailyScoreDao(): DailyScoreDao
    abstract fun weeklyAwardDao(): WeeklyAwardDao
    abstract fun streakDao(): StreakDao
    abstract fun goalDao(): GoalDao
    abstract fun muhosabaDao(): MuhosabaDao

    companion object {
        const val NAME = "reja24.db"
        const val VERSION = 3
    }
}
