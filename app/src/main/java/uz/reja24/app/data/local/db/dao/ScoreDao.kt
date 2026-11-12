package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.DailyScoreEntity
import uz.reja24.app.data.local.db.entity.StreakRecordEntity
import uz.reja24.app.data.local.db.entity.WeeklyAwardEntity
import java.time.LocalDate

@Dao
interface DailyScoreDao {

    @Query("SELECT * FROM daily_scores WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyScoreEntity?>

    @Query("SELECT * FROM daily_scores WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyScoreEntity>>

    @Query("SELECT * FROM daily_scores WHERE date = :date")
    suspend fun get(date: LocalDate): DailyScoreEntity?

    @Query("SELECT * FROM daily_scores WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getRange(from: LocalDate, to: LocalDate): List<DailyScoreEntity>

    @Upsert
    suspend fun upsert(score: DailyScoreEntity)

    /** Kun uchun qator bo'lmasa yaratadi, bo'lsa tegmaydi. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(score: DailyScoreEntity)

    @Query("UPDATE daily_scores SET bypassCount = bypassCount + 1 WHERE date = :date")
    suspend fun incrementBypass(date: LocalDate)

    @Query("SELECT bypassCount FROM daily_scores WHERE date = :date")
    suspend fun getBypassCount(date: LocalDate): Int?

    @Query("UPDATE daily_scores SET wokeOnTime = 1 WHERE date = :date")
    suspend fun markWokeOnTime(date: LocalDate)

    @Query("UPDATE daily_scores SET usedPhoneAfterSleep = 1 WHERE date = :date")
    suspend fun markUsedPhoneAfterSleep(date: LocalDate)
}

@Dao
interface WeeklyAwardDao {

    @Query("SELECT * FROM weekly_awards ORDER BY weekStartDate DESC")
    fun observeAll(): Flow<List<WeeklyAwardEntity>>

    @Query("SELECT * FROM weekly_awards WHERE weekStartDate = :weekStartDate")
    suspend fun get(weekStartDate: LocalDate): WeeklyAwardEntity?

    @Upsert
    suspend fun upsert(award: WeeklyAwardEntity)

    @Query("SELECT COUNT(*) FROM weekly_awards WHERE isPerfectWeek = 1")
    fun observePerfectWeekCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM weekly_awards WHERE isPerfectWeek = 1")
    suspend fun getPerfectWeekCount(): Int
}

@Dao
interface StreakDao {

    @Query("SELECT * FROM streak_records ORDER BY date")
    fun observeAll(): Flow<List<StreakRecordEntity>>

    @Query("SELECT * FROM streak_records ORDER BY date")
    suspend fun getAll(): List<StreakRecordEntity>

    @Upsert
    suspend fun upsert(record: StreakRecordEntity)
}
