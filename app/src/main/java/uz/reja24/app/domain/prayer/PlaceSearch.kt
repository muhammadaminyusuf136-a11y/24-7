package uz.reja24.app.domain.prayer

import uz.reja24.app.domain.model.AppLanguage
import java.text.Normalizer
import java.util.Locale

/** Qidiruv natijasi — tanlansa ManualPlace bo'ladi. */
data class PlaceResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val population: Long = 0,
    val cityId: String? = null
) {
    fun toManualPlace() = ManualPlace(name, latitude, longitude, countryCode, cityId)
}

/** Internetsiz bazadagi bitta shahar (GeoNames). keys — qidirish uchun normallashtirilgan nomlar. */
class PlaceRecord(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val population: Long,
    val nameRu: String,
    val nameAr: String,
    val keys: List<String>
) {
    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.RUSSIAN -> nameRu.ifEmpty { name }
        AppLanguage.ARABIC -> nameAr.ifEmpty { name }
        else -> name
    }

    companion object {
        /** assets/cities.tsv qatori: nom, kenglik, uzunlik, davlat, aholi, ruscha, arabcha, kalitlar(|). */
        fun parse(line: String): PlaceRecord? {
            val p = line.split('\t')
            if (p.size < 8) return null
            val lat = p[1].toDoubleOrNull() ?: return null
            val lon = p[2].toDoubleOrNull() ?: return null
            return PlaceRecord(
                name = p[0],
                latitude = lat,
                longitude = lon,
                countryCode = p[3],
                population = p[4].toLongOrNull() ?: 0,
                nameRu = p[5],
                nameAr = p[6],
                keys = p[7].split('|').filter { it.isNotEmpty() }
            )
        }
    }
}

/**
 * Shahar qidiruvi (sof mantiq — testlanadi).
 * Avval ichki ro'yxat (nomlari 4 tilda), keyin GeoNames bazasi. Nom boshidan mos kelganlar
 * oldinda, katta shaharlar yuqorida.
 */
object PlaceSearch {

    const val MIN_QUERY = 2
    const val LIMIT = 40

    /**
     * Qidirish uchun nomni bir xil ko'rinishga keltiradi: kichik harf, urg'u va apostroflarsiz
     * ("Margʻilon" = "Marg'ilon" = "margilon"), ё → е, tire va nuqta — bo'sh joy.
     * Baza tayyorlangan skript bilan AYNAN bir xil bo'lishi shart.
     */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
            if (ch in APOSTROPHES) continue
            var c = ch.lowercaseChar()
            if (c == 'ё') c = 'е'
            if (c == '-' || c == '_' || c == '.' || c == ',') c = ' '
            sb.append(c)
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun search(query: String, language: AppLanguage, records: List<PlaceRecord>): List<PlaceResult> {
        val q = normalize(query)
        if (q.length < MIN_QUERY) return emptyList()

        // 1) Ichki ro'yxat — nomlari foydalanuvchi tilida
        val builtIn = Cities.ALL.mapNotNull { city ->
            val names = listOf(city.nameUz, city.nameEn, city.nameRu, city.nameAr).map(::normalize)
            val rank = rankOf(q, names) ?: return@mapNotNull null
            rank to PlaceResult(city.name(language), city.latitude, city.longitude, city.countryCode, BUILT_IN_POPULATION, city.id)
        }

        // 2) GeoNames bazasi
        val fromBase = records.mapNotNull { r ->
            val rank = rankOf(q, r.keys) ?: return@mapNotNull null
            rank to PlaceResult(r.displayName(language), r.latitude, r.longitude, r.countryCode, r.population)
        }

        val merged = (builtIn + fromBase)
            .sortedWith(compareBy<Pair<Int, PlaceResult>> { it.first }.thenByDescending { it.second.population })
            .map { it.second }
        // Bir joy ikki manbada bo'lsa (masalan Toshkent) — ichki ro'yxatdagisi qoladi
        val result = mutableListOf<PlaceResult>()
        for (candidate in merged) {
            val duplicate = result.any {
                it.countryCode.equals(candidate.countryCode, ignoreCase = true) &&
                    Cities.distanceKm(it.latitude, it.longitude, candidate.latitude, candidate.longitude) < DUPLICATE_KM
            }
            if (!duplicate) result += candidate
            if (result.size >= LIMIT) break
        }
        return result
    }

    /** 0 — nom to'liq mos, 1 — nom boshidan, 2 — so'z boshidan, 3 — ichida; null — mos emas. */
    private fun rankOf(q: String, names: List<String>): Int? {
        var best: Int? = null
        for (n in names) {
            val r = when {
                n == q -> 0
                n.startsWith(q) -> 1
                n.contains(" $q") -> 2
                q.length >= 3 && n.contains(q) -> 3
                else -> null
            } ?: continue
            if (best == null || r < best) best = r
            if (best == 0) break
        }
        return best
    }

    private const val APOSTROPHES = "'`‘’ʻʼ´ʹʿ"
    private const val DUPLICATE_KM = 15.0
    /** Ichki ro'yxatdagi shaharlar bir xil moslikda bazadagilardan oldin tursin. */
    private const val BUILT_IN_POPULATION = Long.MAX_VALUE

    /** Davlat nomi foydalanuvchi tilida (masalan "UZ" → "Oʻzbekiston"). */
    fun countryName(countryCode: String?, locale: Locale): String =
        countryCode?.takeIf { it.length == 2 }?.let { code ->
            runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(locale) }.getOrNull()
        }.orEmpty()
}
