package uz.reja24.app.domain.model

/**
 * Bir haftaning "bir marta" rejalarini har hafta o'z kunida takrorlanadigan qilish (sof mantiq).
 *  - har bir reja WEEKLY bo'ladi: hafta kuni — o'sha sananing kuni, specificDate — boshlanish sanasi
 *    (shu sanadan oldingi haftalarda ko'rinmaydi);
 *  - keyinroq qo'lda qayta kiritilgan aynan bir xil "bir marta" nusxalar o'chiriladi,
 *    aks holda o'sha kunda reja ikki marta chiqardi.
 */
object WeekRepeat {

    data class Plan(
        val converted: List<ScheduleEntry>,
        val duplicatesToDelete: List<Long>
    )

    /** Haftalik jadvaldagi bir martalik rejalar (har biri bir marta). */
    fun onceEntries(tasks: List<DailyTask>): List<ScheduleEntry> =
        tasks.map { it.entry }
            .filter { it.repeatType == RepeatType.ONCE && it.specificDate != null }
            .distinctBy { it.id }

    fun plan(once: List<ScheduleEntry>, allEntries: List<ScheduleEntry>): Plan {
        val converted = once.mapNotNull { entry ->
            val date = entry.specificDate ?: return@mapNotNull null
            entry.copy(repeatType = RepeatType.WEEKLY, daysOfWeek = setOf(date.dayOfWeek), specificDate = date)
        }
        val convertedIds = converted.map { it.id }.toSet()
        val duplicates = allEntries.filter { other ->
            val otherDate = other.specificDate ?: return@filter false
            other.id !in convertedIds &&
                other.repeatType == RepeatType.ONCE &&
                converted.any { weekly ->
                    val start = weekly.specificDate ?: return@any false
                    otherDate.isAfter(start) &&
                        otherDate.dayOfWeek in weekly.daysOfWeek &&
                        other.startTime == weekly.startTime &&
                        other.endTime == weekly.endTime &&
                        other.title.trim().equals(weekly.title.trim(), ignoreCase = true)
                }
        }.map { it.id }
        return Plan(converted, duplicates)
    }
}
