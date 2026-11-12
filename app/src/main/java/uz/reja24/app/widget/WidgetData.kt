package uz.reja24.app.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.toArgb
import uz.reja24.app.R
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.core.util.TimeProvider
import uz.reja24.app.domain.model.DailyTask
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.PrayerDay
import uz.reja24.app.domain.prayer.PrayerOverview
import uz.reja24.app.domain.prayer.PrayerSchedule
import uz.reja24.app.domain.repository.PrayerSettingsRepository
import uz.reja24.app.domain.repository.ScheduleRepository
import uz.reja24.app.domain.repository.UserPreferencesRepository
import uz.reja24.app.presentation.components.FullDateText
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.components.toComposeColor
import uz.reja24.app.presentation.home.TimelineModel
import java.time.LocalDateTime
import java.time.LocalTime

/** Vidjetdagi bitta qator: reja yoki namoz. */
data class WidgetItem(
    val id: Long,
    val time: String,
    val title: String,
    val color: Int,
    val isNow: Boolean,
    val isDone: Boolean,
    val isPrayer: Boolean
)

data class WidgetData(
    val weekday: String,
    val gregorian: String,
    val hijri: String?,
    val nextPrayerLabel: String,
    val nextPrayer: String?,
    val items: List<WidgetItem>,
    val emptyText: String,
    /** "+%d yana" — sig'magan qatorlar soni uchun (String.format). */
    val moreText: String = "+%d"
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun scheduleRepository(): ScheduleRepository
    fun prayerSettingsRepository(): PrayerSettingsRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun timeProvider(): TimeProvider
}

/** Vidjet uchun ma'lumot: bugungi hozirgi va kelgusi rejalar hamda namozlar (tugaganlari ko'rsatilmaydi). */
object WidgetDataLoader {

    private const val MAX_ITEMS = 12

    suspend fun load(context: Context): WidgetData {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val time = entry.timeProvider()
        val now = time.now()
        val today = now.toLocalDate()
        val prefs = runCatching { entry.userPreferencesRepository().preferences.first() }.getOrNull()
        // Vidjet tizim jarayonida chiziladi — matnlar foydalanuvchi tanlagan tilda olinadi
        val localized = LocaleHelper.localizedContext(context, prefs?.takeIf { it.isLanguageChosen }?.language)
        val res = localized.resources
        val locale = res.configuration.locales[0]

        val tasks = runCatching { entry.scheduleRepository().getTasksForDate(today) }.getOrDefault(emptyList())
        val prayerSettings = runCatching { entry.prayerSettingsRepository().getSettings() }.getOrNull()
        val overview = prayerSettings?.let { PrayerOverview.build(it, time.zone(), listOf(today, today.plusDays(1))) }
        val todayPrayers = overview?.days?.get(today)

        val next = overview?.let { o -> PrayerSchedule.next(o.days.values.sortedBy { it.date }, now) }
        val nextText = next?.let { (prayer, at) -> "${localized.getString(prayerNameRes(prayer))} ${TimeFormat.time(at.toLocalTime())}" }

        return WidgetData(
            weekday = TimeFormat.weekday(today, locale).uppercase(locale),
            gregorian = FullDateText.gregorian(res, today),
            hijri = FullDateText.hijri(res, today, prayerSettings?.hijriAdjustmentDays ?: 0),
            nextPrayerLabel = localized.getString(R.string.widget_next_prayer),
            nextPrayer = nextText,
            items = buildItems(tasks, todayPrayers, now) { localized.getString(prayerNameRes(it)) },
            emptyText = localized.getString(if (tasks.isEmpty()) R.string.widget_empty else R.string.widget_all_done),
            moreText = localized.getString(R.string.widget_more).replace("%1\$s", "%d")
        )
    }

    /** Hozir davom etayotgan va hali boshlanmagan rejalar + kelgusi namozlar, vaqt bo'yicha. */
    internal fun buildItems(
        tasks: List<DailyTask>,
        prayers: PrayerDay?,
        now: LocalDateTime,
        prayerName: (Prayer) -> String
    ): List<WidgetItem> {
        val nowTime = now.toLocalTime()
        val items = mutableListOf<Pair<LocalTime, WidgetItem>>()
        tasks.forEach { task ->
            val e = task.entry
            val start = e.startTime
            // Bugun boshlanadigan nusxa: yarim tundan o'tadigani (23:00–06:00) bugun kechqurun boshlanadi
            val started = !nowTime.isBefore(start)
            val ended = started && !e.crossesMidnight && !e.endTime.isAfter(nowTime)
            if (ended) return@forEach
            val running = started
            items += start to WidgetItem(
                id = e.id,
                time = TimeFormat.time(start),
                title = e.title,
                color = e.colorHex.toComposeColor().toArgb(),
                isNow = running,
                isDone = task.isCompleted,
                isPrayer = false
            )
        }
        prayers?.let { day ->
            Prayer.OBLIGATORY.forEach { prayer ->
                val at = day.time(prayer)
                if (at.plusMinutes(TimelineModel.PRAYER_MINUTES.toLong()).isBefore(now)) return@forEach
                val running = !at.isAfter(now)
                items += at.toLocalTime() to WidgetItem(
                    id = -1L - prayer.ordinal,
                    time = TimeFormat.time(at.toLocalTime()),
                    title = prayerName(prayer),
                    color = PRAYER_ARGB,
                    isNow = running,
                    isDone = false,
                    isPrayer = true
                )
            }
        }
        return items.sortedBy { it.first }.map { it.second }.take(MAX_ITEMS)
    }

    private const val PRAYER_ARGB = 0xFF2B7A5E.toInt()

}
