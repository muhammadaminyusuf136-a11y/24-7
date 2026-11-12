package uz.reja24.app.core.util

import android.content.res.Resources
import uz.reja24.app.R

/** Daqiqalarni o'qiladigan matnga aylantiradi: "1 soat", "30 daqiqa", "1 soat 15 daqiqa". */
object DurationText {

    fun format(resources: Resources, totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        val hoursText = resources.getQuantityString(R.plurals.duration_hours, hours, hours.toString())
        val minutesText = resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes.toString())
        return when {
            hours > 0 && minutes > 0 -> resources.getString(R.string.duration_hours_minutes, hoursText, minutesText)
            hours > 0 -> hoursText
            else -> minutesText
        }
    }

    fun days(resources: Resources, days: Int): String =
        resources.getQuantityString(R.plurals.days_count, days, days.toString())
}
