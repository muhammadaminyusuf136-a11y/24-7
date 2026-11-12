package uz.reja24.app.domain.model

/** Ilova tili. tag — Android locale kodi. */
enum class AppLanguage(val tag: String) {
    UZBEK("uz"),
    ENGLISH("en"),
    RUSSIAN("ru"),
    ARABIC("ar");

    val isRtl: Boolean get() = this == ARABIC

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: UZBEK
    }
}

/** Orqa fon uslubi: Kuch / Nafosat / Sof. */
enum class BackgroundStyle {
    POWER,   // Kuch — to'q ko'k-grafit, diagonal chiziqlar
    GRACE,   // Nafosat — pushti-binafsha, to'lqinlar
    PURE;    // Sof — gradientsiz

    companion object {
        fun fromName(name: String?): BackgroundStyle = entries.firstOrNull { it.name == name } ?: PURE
    }
}

/** Ovozli eslatma ovozi. */
enum class VoiceGender {
    FEMALE,
    MALE;

    companion object {
        fun fromName(name: String?): VoiceGender = entries.firstOrNull { it.name == name } ?: FEMALE
    }
}

/** DataStore'da saqlanadigan oddiy sozlamalar. */
data class UserPreferences(
    val language: AppLanguage = AppLanguage.UZBEK,
    val isLanguageChosen: Boolean = false,   // foydalanuvchi tilni o'zi tanlaganmi
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE,
    val isOnboardingCompleted: Boolean = false,
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    /** Tungi "faqat kerakli" rejim: bloklash vaqtida faqat allowedPackages ochiq (tizim ilovalaridan tashqari). */
    val nightAllowOnly: Boolean = false,
    val allowedPackages: Set<String> = emptySet()
)
