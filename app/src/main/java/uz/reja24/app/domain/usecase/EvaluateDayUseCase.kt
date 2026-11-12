package uz.reja24.app.domain.usecase

import uz.reja24.app.domain.model.DailyScore
import uz.reja24.app.domain.model.ScoreCalculator
import uz.reja24.app.domain.model.StreakRecord
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.domain.model.WeeklyEvaluator
import uz.reja24.app.domain.repository.AppStateRepository
import uz.reja24.app.domain.repository.RoutineSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.ScoreRepository
import uz.reja24.app.domain.repository.StreakRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Yopilgan "rejim kun"larini baholaydi: ballni saqlaydi, streak yozadi,
 * yakshanba bo'lsa haftani baholaydi. Yangi kubok olinsa — uni qaytaradi.
 *
 * Kun ertasi tongda, uyg'onish vaqtida yopiladi (tungi voqealar ham hisobga kirsin).
 */
class EvaluateDayUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scoreRepository: ScoreRepository,
    private val streakRepository: StreakRepository,
    private val routineSettingsRepository: RoutineSettingsRepository,
    private val appStateRepository: AppStateRepository
) {

    /**
     * `openDay` dan oldingi, hali baholanmagan barcha kunlarni tartib bilan baholaydi
     * (telefon o'chiq bo'lgan yoki tizim vazifani to'xtatgan kunlar ham). Eng ko'pi bilan 60 kun.
     * Qayta chaqirilsa zarar yo'q — allaqachon baholangan kunlar qayta hisoblanmaydi.
     */
    suspend fun catchUp(openDay: LocalDate): TrophyLevel? {
        val last = appStateRepository.getLastEvaluatedDate()
        if (last == null) {
            // Birinchi ishga tushish — o'tmish yo'q
            appStateRepository.setLastEvaluatedDate(openDay.minusDays(1))
            return null
        }
        var date = last.plusDays(1)
        if (ChronoUnit.DAYS.between(date, openDay) > MAX_CATCH_UP_DAYS) {
            date = openDay.minusDays(MAX_CATCH_UP_DAYS)
        }
        var trophy: TrophyLevel? = null
        while (date.isBefore(openDay)) {
            evaluate(date)?.let { trophy = it }
            appStateRepository.setLastEvaluatedDate(date)
            date = date.plusDays(1)
        }
        return trophy
    }

    private suspend fun evaluate(date: LocalDate): TrophyLevel? {
        val settings = routineSettingsRepository.getSettings()
        val tasks = scheduleRepository.getTasksForDate(date)
        val existing = scoreRepository.getDailyScore(date) ?: DailyScore(date = date)

        val score = ScoreCalculator.calculate(
            date = date,
            completedTasks = tasks.count { it.isCompleted },
            totalTasks = tasks.size,
            isRoutineActive = settings.isRoutineActive,
            bypassCount = existing.bypassCount,
            usedPhoneAfterSleep = existing.usedPhoneAfterSleep,
            wokeOnTime = existing.wokeOnTime
        )
        scoreRepository.saveDailyScore(score)
        streakRepository.saveRecord(StreakRecord(date = date, wasSuccessful = score.routineKept))

        return if (date.dayOfWeek == DayOfWeek.SUNDAY) {
            evaluateWeek(WeeklyEvaluator.weekStartOf(date))
        } else {
            null
        }
    }

    private suspend fun evaluateWeek(weekStart: LocalDate): TrophyLevel? {
        val scores = scoreRepository.getDailyScores(weekStart, weekStart.plusDays(6))
        val alreadySaved = scoreRepository.getWeeklyAward(weekStart)
        // Hafta qayta baholansa, o'zini ikki marta sanamasligi uchun
        val previousPerfect = scoreRepository.getPerfectWeekCount() -
            if (alreadySaved?.isPerfectWeek == true) 1 else 0
        val award = WeeklyEvaluator.evaluate(weekStart, scores, previousPerfect)
        scoreRepository.saveWeeklyAward(award)

        val previousLevel = TrophyLevel.forPerfectWeeks(previousPerfect)
        // Kubok faqat bir marta tabriklanadi (hafta qayta baholansa — takror chiqmaydi)
        val isNewTrophy = award.isPerfectWeek &&
            alreadySaved?.isPerfectWeek != true &&
            award.trophyLevel != previousLevel
        return if (isNewTrophy) {
            appStateRepository.setPendingTrophy(award.trophyLevel)
            award.trophyLevel
        } else {
            null
        }
    }

    companion object {
        const val MAX_CATCH_UP_DAYS = 60L
    }
}
