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
    val isPrayer: Boolean,
    /** Vaqti o'tib ketgan — ro'yxatdan o'chmaydi, kulrang ko'rsatiladi. */
    val isPast: Boolean = false
)

/** Vidjetda ko'rinadigan qism: tepada yashirilgan o'tganlar soni, ko'rinadiganlar, pastda yashirilganlar soni. */
data class WidgetWindow(val hiddenBefore: Int, val shown: List<WidgetItem>, val hiddenAfter: Int)

data class WidgetData(
    val weekday: String,
    val gregorian: String,
    val hijri: String?,
    val nextPrayerLabel: String,
    val nextPrayer: String?,
    val items: List<WidgetItem>,
    val emptyText: String,
    /** "+%d yana" — sig'magan qatorlar soni uchun (String.format). */
    val moreText: String = "+%d",
    val earlierText: String = "%d"
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

    private const val MAX_ITEMS = 24

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
            moreText = localized.getString(R.string.widget_more).replace("%1\$s", "%d"),
            earlierText = localized.getString(R.string.widget_earlier).replace("%1\$s", "%d")
        )
    }

    /**
     * Bugungi barcha rejalar va namozlar, vaqt bo'yicha. O'tib ketganlari ham qoladi (isPast) —
     * vidjetda kulrang ko'rinadi, kun tartibidan o'chib ketmaydi.
     */
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
            val running = started && !ended
            items += start to WidgetItem(
                id = e.id,
                time = TimeFormat.time(start),
                title = e.title,
                color = e.colorHex.toComposeColor().toArgb(),
                isNow = running,
                isDone = task.isCompleted,
                isPrayer = false,
                isPast = ended
            )
        }
        prayers?.let { day ->
            Prayer.OBLIGATORY.forEach { prayer ->
                val at = day.time(prayer)
                val ended = at.plusMinutes(TimelineModel.PRAYER_MINUTES.toLong()).isBefore(now)
                val running = !ended && !at.isAfter(now)
                items += at.toLocalTime() to WidgetItem(
                    id = -1L - prayer.ordinal,
                    time = TimeFormat.time(at.toLocalTime()),
                    title = prayerName(prayer),
                    color = PRAYER_ARGB,
                    isNow = running,
                    isDone = false,
                    isPrayer = true,
                    isPast = ended
                )
            }
        }
        return items.sortedBy { it.first }.map { it.second }.take(MAX_ITEMS)
    }

    /**
     * Vidjetga sig'maganda qaysi qatorlar ko'rinishini tanlaydi: hozirgi va keyingi rejalar doim
     * ko'rinadi, ulardan oldin eng yaqin o'tgan reja (agar joy bo'lsa, ko'proq o'tganlar).
     * rowsFor(n) — n ta "yana ..." qatori bo'lganda nechta reja qatori sig'adi.
     */
    internal fun window(items: List<WidgetItem>, rowsFor: (Int) -> Int): WidgetWindow {
        if (items.size <= rowsFor(0)) return WidgetWindow(0, items, 0)
        val firstActive = items.indexOfFirst { !it.isPast }.let { if (it < 0) items.size else it }
        fun build(lines: Int): WidgetWindow {
            val rows = rowsFor(lines).coerceAtLeast(1)
            val start = (firstActive - 1).coerceAtMost(items.size - rows).coerceAtLeast(0)
            val end = (start + rows).coerceAtMost(items.size)
            return WidgetWindow(start, items.subList(start, end), items.size - end)
        }
        val both = build(2)
        val lines = (if (both.hiddenBefore > 0) 1 else 0) + (if (both.hiddenAfter > 0) 1 else 0)
        return if (lines < 2) build(lines) else both
    }

    private const val PRAYER_ARGB = 0xFF2B7A5E.toInt()

}
