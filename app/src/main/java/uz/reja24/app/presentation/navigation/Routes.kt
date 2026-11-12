package uz.reja24.app.presentation.navigation

import kotlinx.serialization.Serializable

/** Ilovadagi barcha ekranlar manzillari (type-safe navigatsiya). */
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Today : Route
    @Serializable data object Plan : Route
    @Serializable data object Results : Route
    @Serializable data object Settings : Route

    /** entryId = 0 — yangi reja; dateEpochDay — qaysi kunga (NO_DATE — bugun). */
    @Serializable data class EntryEditor(
        val entryId: Long = 0L,
        val dateEpochDay: Long = NO_DATE
    ) : Route

    @Serializable data object AppPicker : Route
    @Serializable data object Reliability : Route
    @Serializable data object Privacy : Route
    @Serializable data object PrayerSettings : Route
    @Serializable data object AllowedApps : Route
    /** Kechki muhosaba oynasi: hikmat + "Hisobni boshlash". */
    @Serializable data object MuhosabaEvening : Route

    companion object {
        const val NO_DATE = Long.MIN_VALUE
    }
}
