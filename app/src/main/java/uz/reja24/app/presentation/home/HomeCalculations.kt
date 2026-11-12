package uz.reja24.app.presentation.home

import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.model.RoutineSettings
import java.time.LocalDateTime

/** "Keyingi" kartasidagi element: reja yoki rejim (bloklash) boshlanishi. */
data class NextItem(
    val title: String?,          // null — bu rejim (bloklash) boshlanishi
    val startsAt: LocalDateTime
)

/** Bosh sahifa hisob-kitoblari — sof funksiyalar (testlanadi). */
object HomeCalculations {

    /** Hozir davom etayotgan reja. */
    fun currentTask(tasks: List<DailyTask>, now: LocalDateTime): DailyTask? =
        tasks.firstOrNull { task ->
            val start = task.date.atTime(task.entry.startTime)
            val end = start.plusMinutes(task.entry.durationMinutes.toLong())
            !now.isBefore(start) && now.isBefore(end)
        }

    /** Hozirgi reja tugashiga qolgan daqiqalar. */
    fun minutesLeft(task: DailyTask, now: LocalDateTime): Long {
        val end = task.date.atTime(task.entry.startTime).plusMinutes(task.entry.durationMinutes.toLong())
        return java.time.Duration.between(now, end).toMinutes().coerceAtLeast(0)
    }

    /** Eng yaqin keyingi reja; bugun reja qolmagan bo'lsa — rejim boshlanishi. */
    fun nextItem(tasks: List<DailyTask>, settings: RoutineSettings, now: LocalDateTime): NextItem? {
        val nextTask = tasks
            .map { it to it.date.atTime(it.entry.startTime) }
            .filter { (task, start) -> start.isAfter(now) && !task.isCompleted }
            .minByOrNull { it.second }
        val blockStart = if (settings.isRoutineActive) {
            val today = now.toLocalDate().atTime(settings.blockStartTime)
            if (today.isAfter(now)) today else today.plusDays(1)
        } else {
            null
        }
        return when {
            nextTask != null && (blockStart == null || !nextTask.second.isAfter(blockStart)) ->
                NextItem(nextTask.first.entry.title, nextTask.second)
            blockStart != null -> NextItem(null, blockStart)
            else -> null
        }
    }
}
