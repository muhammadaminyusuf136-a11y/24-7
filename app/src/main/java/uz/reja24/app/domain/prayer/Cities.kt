package uz.reja24.app.domain.prayer

import uz.reja24.app.domain.model.AppLanguage
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ichki shaharlar ro'yxati (internetsiz ishlash uchun).
 * zone — shu vaqt zonasining "asosiy" shahri bo'lsa to'ldiriladi: joylashuv ruxsati
 * bo'lmaganda telefon vaqt zonasidan shahar taxmin qilinadi.
 */
data class City(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val nameUz: String,
    val nameEn: String,
    val nameRu: String,
    val nameAr: String,
    val zone: String? = null
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.UZBEK -> nameUz
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.RUSSIAN -> nameRu
        AppLanguage.ARABIC -> nameAr
    }
}

object Cities {

    /** Eng yaqin shahar shu masofadan uzoq bo'lsa, nomi "joriy joylashuv" deb ko'rsatiladi. */
    const val NEAR_CITY_KM = 80.0

    val ALL: List<City> = listOf(
        // O'zbekiston
        City("tashkent", 41.2995, 69.2401, "UZ", "Toshkent", "Tashkent", "Ташкент", "طشقند", "Asia/Tashkent"),
        City("samarkand", 39.6542, 66.9597, "UZ", "Samarqand", "Samarkand", "Самарканд", "سمرقند", "Asia/Samarkand"),
        City("bukhara", 39.7747, 64.4286, "UZ", "Buxoro", "Bukhara", "Бухара", "بخارى"),
        City("andijan", 40.7821, 72.3442, "UZ", "Andijon", "Andijan", "Андижан", "أنديجان"),
        City("namangan", 40.9983, 71.6726, "UZ", "Namangan", "Namangan", "Наманган", "نمنغان"),
        City("fergana", 40.3864, 71.7864, "UZ", "Fargʻona", "Fergana", "Фергана", "فرغانة"),
        City("kokand", 40.5286, 70.9425, "UZ", "Qoʻqon", "Kokand", "Коканд", "خوقند"),
        City("margilan", 40.4717, 71.7247, "UZ", "Margʻilon", "Margilan", "Маргилан", "مرغيلان"),
        City("chirchiq", 41.4689, 69.5822, "UZ", "Chirchiq", "Chirchiq", "Чирчик", "تشيرتشيك"),
        City("qarshi", 38.8606, 65.7891, "UZ", "Qarshi", "Karshi", "Карши", "قرشي"),
        City("termez", 37.2242, 67.2783, "UZ", "Termiz", "Termez", "Термез", "ترمذ"),
        City("jizzakh", 40.1158, 67.8422, "UZ", "Jizzax", "Jizzakh", "Джизак", "جيزاخ"),
        City("gulistan", 40.4897, 68.7842, "UZ", "Guliston", "Gulistan", "Гулистан", "غولستان"),
        City("navoiy", 40.1039, 65.3739, "UZ", "Navoiy", "Navoiy", "Навои", "نوائي"),
        City("urgench", 41.5500, 60.6333, "UZ", "Urganch", "Urgench", "Ургенч", "أورغنج"),
        City("khiva", 41.3783, 60.3639, "UZ", "Xiva", "Khiva", "Хива", "خيوة"),
        City("nukus", 42.4600, 59.6100, "UZ", "Nukus", "Nukus", "Нукус", "نوكوس"),
        // Markaziy Osiyo va Kavkaz
        City("almaty", 43.2389, 76.8897, "KZ", "Olmaota", "Almaty", "Алматы", "ألماتي", "Asia/Almaty"),
        City("astana", 51.1694, 71.4491, "KZ", "Ostona", "Astana", "Астана", "أستانا"),
        City("shymkent", 42.3417, 69.5901, "KZ", "Chimkent", "Shymkent", "Шымкент", "شيمكنت"),
        City("bishkek", 42.8746, 74.5698, "KG", "Bishkek", "Bishkek", "Бишкек", "بيشكك", "Asia/Bishkek"),
        City("osh", 40.5140, 72.8161, "KG", "Oʻsh", "Osh", "Ош", "أوش"),
        City("jalal_abad", 40.9333, 72.9833, "KG", "Jalolobod", "Jalal-Abad", "Джалал-Абад", "جلال آباد"),
        City("dushanbe", 38.5598, 68.7870, "TJ", "Dushanbe", "Dushanbe", "Душанбе", "دوشنبه", "Asia/Dushanbe"),
        City("khujand", 40.2826, 69.6222, "TJ", "Xoʻjand", "Khujand", "Худжанд", "خجند"),
        City("ashgabat", 37.9601, 58.3261, "TM", "Ashxobod", "Ashgabat", "Ашхабад", "عشق آباد", "Asia/Ashgabat"),
        City("kabul", 34.5553, 69.2075, "AF", "Kobul", "Kabul", "Кабул", "كابول", "Asia/Kabul"),
        City("baku", 40.4093, 49.8671, "AZ", "Boku", "Baku", "Баку", "باكو", "Asia/Baku"),
        // Rossiya
        City("moscow", 55.7558, 37.6173, "RU", "Moskva", "Moscow", "Москва", "موسكو", "Europe/Moscow"),
        City("saint_petersburg", 59.9311, 30.3609, "RU", "Sankt-Peterburg", "Saint Petersburg", "Санкт-Петербург", "سانت بطرسبرغ"),
        City("kazan", 55.7963, 49.1088, "RU", "Qozon", "Kazan", "Казань", "قازان"),
        City("ufa", 54.7388, 55.9721, "RU", "Ufa", "Ufa", "Уфа", "أوفا"),
        City("makhachkala", 42.9849, 47.5047, "RU", "Mahachqalʼa", "Makhachkala", "Махачкала", "محج قلعة"),
        City("grozny", 43.3178, 45.6949, "RU", "Grozniy", "Grozny", "Грозный", "غروزني"),
        City("yekaterinburg", 56.8389, 60.6057, "RU", "Yekaterinburg", "Yekaterinburg", "Екатеринбург", "يكاترينبورغ", "Asia/Yekaterinburg"),
        City("novosibirsk", 55.0084, 82.9357, "RU", "Novosibirsk", "Novosibirsk", "Новосибирск", "نوفوسيبيرسك", "Asia/Novosibirsk"),
        // Turkiya
        City("istanbul", 41.0082, 28.9784, "TR", "Istanbul", "Istanbul", "Стамбул", "إسطنبول", "Europe/Istanbul"),
        City("ankara", 39.9334, 32.8597, "TR", "Anqara", "Ankara", "Анкара", "أنقرة"),
        // Arab davlatlari
        City("makkah", 21.4225, 39.8262, "SA", "Makka", "Makkah", "Мекка", "مكة المكرمة"),
        City("madinah", 24.4686, 39.6142, "SA", "Madina", "Madinah", "Медина", "المدينة المنورة"),
        City("riyadh", 24.7136, 46.6753, "SA", "Ar-Riyod", "Riyadh", "Эр-Рияд", "الرياض", "Asia/Riyadh"),
        City("jeddah", 21.4858, 39.1925, "SA", "Jidda", "Jeddah", "Джидда", "جدة"),
        City("dubai", 25.2048, 55.2708, "AE", "Dubay", "Dubai", "Дубай", "دبي", "Asia/Dubai"),
        City("abu_dhabi", 24.4539, 54.3773, "AE", "Abu-Dabi", "Abu Dhabi", "Абу-Даби", "أبوظبي"),
        City("doha", 25.2854, 51.5310, "QA", "Doha", "Doha", "Доха", "الدوحة", "Asia/Qatar"),
        City("kuwait", 29.3759, 47.9774, "KW", "Kuvayt", "Kuwait City", "Эль-Кувейт", "مدينة الكويت", "Asia/Kuwait"),
        City("manama", 26.2285, 50.5860, "BH", "Manama", "Manama", "Манама", "المنامة", "Asia/Bahrain"),
        City("muscat", 23.5880, 58.3829, "OM", "Maskat", "Muscat", "Маскат", "مسقط", "Asia/Muscat"),
        City("sanaa", 15.3694, 44.1910, "YE", "Sano", "Sanaa", "Сана", "صنعاء", "Asia/Aden"),
        City("cairo", 30.0444, 31.2357, "EG", "Qohira", "Cairo", "Каир", "القاهرة", "Africa/Cairo"),
        City("alexandria", 31.2001, 29.9187, "EG", "Iskandariya", "Alexandria", "Александрия", "الإسكندرية"),
        City("amman", 31.9454, 35.9284, "JO", "Ammon", "Amman", "Амман", "عمّان", "Asia/Amman"),
        City("jerusalem", 31.7683, 35.2137, "PS", "Quddus", "Jerusalem", "Иерусалим", "القدس", "Asia/Jerusalem"),
        City("beirut", 33.8938, 35.5018, "LB", "Bayrut", "Beirut", "Бейрут", "بيروت", "Asia/Beirut"),
        City("damascus", 33.5138, 36.2765, "SY", "Damashq", "Damascus", "Дамаск", "دمشق", "Asia/Damascus"),
        City("baghdad", 33.3152, 44.3661, "IQ", "Bagʻdod", "Baghdad", "Багдад", "بغداد", "Asia/Baghdad"),
        City("khartoum", 15.5007, 32.5599, "SD", "Xartum", "Khartoum", "Хартум", "الخرطوم", "Africa/Khartoum"),
        City("tripoli", 32.8872, 13.1913, "LY", "Tripoli", "Tripoli", "Триполи", "طرابلس", "Africa/Tripoli"),
        City("tunis", 36.8065, 10.1815, "TN", "Tunis", "Tunis", "Тунис", "تونس", "Africa/Tunis"),
        City("algiers", 36.7538, 3.0588, "DZ", "Jazoir", "Algiers", "Алжир", "الجزائر", "Africa/Algiers"),
        City("casablanca", 33.5731, -7.5898, "MA", "Kasablanka", "Casablanca", "Касабланка", "الدار البيضاء", "Africa/Casablanca"),
        City("rabat", 34.0209, -6.8416, "MA", "Rabot", "Rabat", "Рабат", "الرباط"),
        City("tehran", 35.6892, 51.3890, "IR", "Tehron", "Tehran", "Тегеран", "طهران", "Asia/Tehran"),
        // Janubiy va Janubi-Sharqiy Osiyo
        City("karachi", 24.8607, 67.0011, "PK", "Karachi", "Karachi", "Карачи", "كراتشي", "Asia/Karachi"),
        City("lahore", 31.5204, 74.3587, "PK", "Lahor", "Lahore", "Лахор", "لاهور"),
        City("islamabad", 33.6844, 73.0479, "PK", "Islomobod", "Islamabad", "Исламабад", "إسلام آباد"),
        City("delhi", 28.6139, 77.2090, "IN", "Dehli", "Delhi", "Дели", "دلهي", "Asia/Kolkata"),
        City("dhaka", 23.8103, 90.4125, "BD", "Dakka", "Dhaka", "Дакка", "دكا", "Asia/Dhaka"),
        City("jakarta", -6.2088, 106.8456, "ID", "Jakarta", "Jakarta", "Джакарта", "جاكرتا", "Asia/Jakarta"),
        City("kuala_lumpur", 3.1390, 101.6869, "MY", "Kuala-Lumpur", "Kuala Lumpur", "Куала-Лумпур", "كوالالمبور", "Asia/Kuala_Lumpur"),
        // Sharqiy Osiyo
        City("seoul", 37.5665, 126.9780, "KR", "Seul", "Seoul", "Сеул", "سيول", "Asia/Seoul"),
        City("tokyo", 35.6762, 139.6503, "JP", "Tokio", "Tokyo", "Токио", "طوكيو", "Asia/Tokyo"),
        // Yevropa
        City("london", 51.5074, -0.1278, "GB", "London", "London", "Лондон", "لندن", "Europe/London"),
        City("paris", 48.8566, 2.3522, "FR", "Parij", "Paris", "Париж", "باريس", "Europe/Paris"),
        City("berlin", 52.5200, 13.4050, "DE", "Berlin", "Berlin", "Берлин", "برلين", "Europe/Berlin"),
        // Amerika
        City("new_york", 40.7128, -74.0060, "US", "Nyu-York", "New York", "Нью-Йорк", "نيويورك", "America/New_York"),
        City("chicago", 41.8781, -87.6298, "US", "Chikago", "Chicago", "Чикаго", "شيكاغو", "America/Chicago"),
        City("los_angeles", 34.0522, -118.2437, "US", "Los-Anjeles", "Los Angeles", "Лос-Анджелес", "لوس أنجلوس", "America/Los_Angeles"),
        City("toronto", 43.6532, -79.3832, "CA", "Toronto", "Toronto", "Торонто", "تورونتو", "America/Toronto")
    )

    private val byId: Map<String, City> = ALL.associateBy { it.id }

    fun byId(id: String?): City? = id?.let { byId[it] }

    /** Telefon vaqt zonasining asosiy shahri (masalan Asia/Tashkent → Toshkent). */
    fun forZone(zoneId: String): City? = ALL.firstOrNull { it.zone == zoneId }

    fun nearest(latitude: Double, longitude: Double): City =
        ALL.minBy { distanceKm(latitude, longitude, it.latitude, it.longitude) }

    /** Ikki nuqta orasidagi masofa (km), Yer shar deb olinadi. */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6371.0 * asin(sqrt(a))
    }

    /** Mamlakat bo'yicha tavsiya etiladigan hisoblash usuli va asr mazhabi. */
    fun defaultsFor(countryCode: String?): Pair<CalculationMethod, AsrMadhab> = when (countryCode) {
        "UZ", "KZ", "KG", "TJ", "TM", "AF", "AZ", "TR" -> CalculationMethod.MWL to AsrMadhab.HANAFI
        "RU" -> CalculationMethod.RUSSIA to AsrMadhab.HANAFI
        "PK", "IN", "BD" -> CalculationMethod.KARACHI to AsrMadhab.HANAFI
        "SA", "YE", "AE", "QA", "KW", "BH", "OM" -> CalculationMethod.MAKKAH to AsrMadhab.SHAFII
        "EG", "SD", "LY", "SY", "LB", "JO", "PS", "IQ", "TN", "DZ", "MA" -> CalculationMethod.EGYPT to AsrMadhab.SHAFII
        "ID", "MY" -> CalculationMethod.SOUTHEAST_ASIA to AsrMadhab.SHAFII
        "US", "CA" -> CalculationMethod.ISNA to AsrMadhab.SHAFII
        else -> CalculationMethod.MWL to AsrMadhab.SHAFII
    }
}
