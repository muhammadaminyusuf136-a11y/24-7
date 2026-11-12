package uz.reja24.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.RejaDatabase
import uz.reja24.app.data.local.db.dao.DailyScoreDao
import uz.reja24.app.data.local.db.dao.WeeklyAwardDao
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.WeeklyAward
import uz.reja24.app.domain.repository.ScoreRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreRepositoryImpl @Inject constructor(
    private val database: RejaDatabase,
    private val dailyScoreDao: DailyScoreDao,
    private val weeklyAwardDao: WeeklyAwardDao
) : ScoreRepository {

    override fun observeDailyScore(date: LocalDate): Flow<DailyScore?> =
        dailyScoreDao.observe(date).map { it?.toDomain() }

    override fun observeDailyScores(from: LocalDate, to: LocalDate): Flow<List<DailyScore>> =
        dailyScoreDao.observeRange(from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun getDailyScore(date: LocalDate): DailyScore? =
        dailyScoreDao.get(date)?.toDomain()

    override suspend fun getDailyScores(from: LocalDate, to: LocalDate): List<DailyScore> =
        dailyScoreDao.getRange(from, to).map { it.toDomain() }

    override suspend fun saveDailyScore(score: DailyScore) = dailyScoreDao.upsert(score.toEntity())

    /**
     * Favqulodda kirishni qayd qiladi. Tranzaksiya ichida — ikki marta tez bosilsa ham
     * hisob adashmaydi. Ball ayirish 7-bosqichdagi hisoblash mantiqida bajariladi.
     */
    override suspend fun registerBypass(date: LocalDate): Int = database.withTransaction {
        dailyScoreDao.insertIfAbsent(DailyScore(date = date).toEntity())
        dailyScoreDao.incrementBypass(date)
        dailyScoreDao.getBypassCount(date) ?: 0
    }

    override suspend fun markWokeOnTime(date: LocalDate) = database.withTransaction {
        dailyScoreDao.insertIfAbsent(DailyScore(date = date).toEntity())
        dailyScoreDao.markWokeOnTime(date)
    }

    override suspend fun markPhoneUsedAfterSleep(date: LocalDate) = database.withTransaction {
        dailyScoreDao.insertIfAbsent(DailyScore(date = date).toEntity())
        dailyScoreDao.markUsedPhoneAfterSleep(date)
    }

    override fun observeWeeklyAwards(): Flow<List<WeeklyAward>> =
        weeklyAwardDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getWeeklyAward(weekStartDate: LocalDate): WeeklyAward? =
        weeklyAwardDao.get(weekStartDate)?.toDomain()

    override suspend fun saveWeeklyAward(award: WeeklyAward) = weeklyAwardDao.upsert(award.toEntity())

    override fun observePerfectWeekCount(): Flow<Int> = weeklyAwardDao.observePerfectWeekCount()

    override suspend fun getPerfectWeekCount(): Int = weeklyAwardDao.getPerfectWeekCount()
}
