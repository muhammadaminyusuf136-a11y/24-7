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

/**
 * Ilova rangi (mavzu va fon bitta tanlov). Nomlar bazada saqlangani uchun enum nomlari eski.
 */
enum class BackgroundStyle {
    PURE,    // Qaymoq — ko'zni og'ritmaydigan kulrang-qaymoq, yorug'
    POWER,   // To'q ko'k
    GREEN,   // To'q yashil
    GRACE;   // To'q pushti

    companion object {
        fun fromName(name: String?): BackgroundStyle = entries.firstOrNull { it.name == name } ?: PURE
    }
}

/** Sanalar qanday ko'rsatiladi: ikkalasi, faqat milodiy yoki faqat hijriy. */
enum class DateDisplay {
    BOTH,
    GREGORIAN,
    HIJRI;

    companion object {
        fun fromName(name: String?): DateDisplay = entries.firstOrNull { it.name == name } ?: BOTH
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
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE,
    val dateDisplay: DateDisplay = DateDisplay.BOTH,
    val isOnboardingCompleted: Boolean = false,
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    /** Tungi "faqat kerakli" rejim: bloklash vaqtida faqat allowedPackages ochiq (tizim ilovalaridan tashqari). */
    val nightAllowOnly: Boolean = false,
    /** Rejim vaqtida ekran oq-qora bo'ladi. */
    val nightGrayscale: Boolean = false,
    val allowedPackages: Set<String> = emptySet(),
    /** Rejalar eslatmasi ovoz chiqarib o'qiladi ("Sport boshlanishiga 10 daqiqa qoldi"). */
    val taskVoiceOn: Boolean = false,
    /** Ikkinchi eslatma: boshlanishidan shuncha daqiqa oldin (0 — yo'q). */
    val taskSecondReminderMinutes: Int = 0
)
