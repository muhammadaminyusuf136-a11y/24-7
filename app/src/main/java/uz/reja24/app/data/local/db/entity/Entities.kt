package uz.reja24.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/*
 * Ma'lumotlar bazasi jadvallari.
 * Diqqat: jadval yoki ustun o'zgarsa, RejaDatabase.version oshiriladi va
 * DatabaseMigrations'ga migratsiya yoziladi — aks holda foydalanuvchi ma'lumoti yo'qoladi.
 */

@Entity(
    tableName = "schedule_entries",
    indices = [Index("isActive")]
)
data class ScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val category: String,
    val colorHex: String,
    val isActive: Boolean,
    val repeatType: String,
    val daysOfWeek: Set<DayOfWeek>,
    val specificDate: LocalDate?,
    val reminderMinutesBefore: Int,
    /** Boshlanish vaqtida budilnik (ovoz o'chirilguncha chaladi). 2-versiyada qo'shilgan. */
    @ColumnInfo(defaultValue = "0") val alarmEnabled: Boolean = false,
    /** Budilnik ovozi; null — telefonning standart budilnik ovozi. */
    val alarmSoundUri: String? = null
)

/** Reja muayyan kuni bajarildi. Reja o'chirilsa — bu yozuvlar ham avtomatik o'chadi. */
@Entity(
    tableName = "task_completions",
    primaryKeys = ["entryId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("date")]
)
data class TaskCompletionEntity(
    val entryId: Long,
    val date: LocalDate,
    val completedAt: Long
)

/** Rejim sozlamalari — jadvalda doim bitta qator (id = 1). */
@Entity(tableName = "routine_settings")
data class RoutineSettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val blockStartTime: LocalTime,
    val sleepTime: LocalTime,
    val wakeTime: LocalTime,
    val isRoutineActive: Boolean,
    val reminderStage1Minutes: Int,
    val reminderStage2Minutes: Int,
    val reminderStage3Minutes: Int,
    val finalWarningMinutes: Int,
    val isVoiceReminderOn: Boolean,
    val ringtoneUri: String?,
    val isStrictModeOn: Boolean
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean
)

@Entity(tableName = "daily_scores")
data class DailyScoreEntity(
    @PrimaryKey val date: LocalDate,
    val points: Int,
    val starsEarned: Int,
    val completedTasks: Int,
    val totalTasks: Int,
    val routineKept: Boolean,
    val sleptOnTime: Boolean,
    val wokeOnTime: Boolean,
    val bypassCount: Int,
    val usedPhoneAfterSleep: Boolean
)

@Entity(tableName = "weekly_awards")
data class WeeklyAwardEntity(
    @PrimaryKey val weekStartDate: LocalDate,
    val totalPoints: Int,
    val averageStars: Float,
    val isPerfectWeek: Boolean,
    val trophyLevel: String
)

@Entity(tableName = "streak_records")
data class StreakRecordEntity(
    @PrimaryKey val date: LocalDate,
    val wasSuccessful: Boolean
)

@Entity(
    tableName = "goals",
    indices = [Index("year")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val year: Int,
    val progressPercent: Int,
    val colorHex: String,
    val isAchieved: Boolean
)

/** Muhosaba (kunlik hisob) ro'yxatidagi band — foydalanuvchi o'zi tuzadi. 3-versiyada qo'shilgan. */
@Entity(tableName = "muhosaba_items")
data class MuhosabaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sortOrder: Int,
    /** O'chirilgan band ro'yxatda ko'rinmaydi, lekin o'tgan kunlardagi belgilari saqlanadi. */
    val isArchived: Boolean
)

/** Bandning muayyan kundagi belgisi: bajarildi (true) yoki bajarilmadi (false). Belgi yo'q — hali hisob qilinmagan. */
@Entity(
    tableName = "muhosaba_marks",
    primaryKeys = ["itemId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = MuhosabaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class MuhosabaMarkEntity(
    val itemId: Long,
    val date: LocalDate,
    val done: Boolean
)
