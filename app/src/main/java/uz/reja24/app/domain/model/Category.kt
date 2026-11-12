package uz.reja24.app.domain.model

/**
 * Reja toifasi. Har bir toifaning o'z standart rangi bor (dizayn tizimidan).
 * Foydalanuvchi rangni o'zgartirishi mumkin — u ScheduleEntry.colorHex'da saqlanadi.
 */
enum class Category(val defaultColorHex: String) {
    WORK("#D9663D"),      // ish
    STUDY("#3E6B5B"),     // dars
    SPORT("#4F7FA3"),     // sport
    FAMILY("#7B8F5A"),    // oila
    READING("#6B6191"),   // mutolaa
    REST("#9C7A5B"),      // dam olish
    SLEEP("#46596B"),     // uyqu
    MORNING("#E0A44A"),   // tong
    OTHER("#B5AEA1");     // boshqa

    companion object {
        /** Bazadagi noma'lum qiymat ilovani buzmasligi uchun xavfsiz o'girish. */
        fun fromName(name: String): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}
