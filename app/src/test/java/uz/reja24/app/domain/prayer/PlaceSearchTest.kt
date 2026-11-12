package uz.reja24.app.domain.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import uz.reja24.app.domain.model.AppLanguage
import java.io.File

/** Haqiqiy baza fayli (src/main/assets/cities.tsv) bilan qidiruv. */
class PlaceSearchTest {

    companion object {
        private lateinit var records: List<PlaceRecord>

        @BeforeClass
        @JvmStatic
        fun load() {
            records = File("src/main/assets/cities.tsv").readLines(Charsets.UTF_8).mapNotNull(PlaceRecord::parse)
        }
    }

    private fun top(query: String, language: AppLanguage = AppLanguage.UZBEK) =
        PlaceSearch.search(query, language, records).first()

    @Test
    fun base_is_loaded() {
        assertTrue("bazada kamida 30 000 shahar", records.size > 30_000)
    }

    @Test
    fun normalization_ignores_apostrophes_accents_and_case() {
        assertEquals("margilon", PlaceSearch.normalize("Margʻilon"))
        assertEquals("margilon", PlaceSearch.normalize("Marg'ilon"))
        assertEquals("qoqon", PlaceSearch.normalize("Qoʻqon"))
        assertEquals("ешкар ола", PlaceSearch.normalize("Ёшкар-Ола"))
        assertEquals("sao paulo", PlaceSearch.normalize("São Paulo"))
    }

    @Test
    fun fergana_valley_towns_are_found_offline() {
        assertEquals("margilan", top("Margʻilon").cityId)
        val rishton = top("rishton")
        assertEquals("UZ", rishton.countryCode)
        assertEquals(40.35667, rishton.latitude, 0.01)
        assertEquals("UZ", top("quva").countryCode)
        assertEquals("KG", top("özgön").countryCode)
    }

    @Test
    fun built_in_cities_come_first_with_localized_names() {
        val tashkent = top("Ташкент", AppLanguage.UZBEK)
        assertEquals("tashkent", tashkent.cityId)
        assertEquals("Toshkent", tashkent.name)
        assertEquals("Ташкент", top("tashkent", AppLanguage.RUSSIAN).name)
        assertEquals("cairo", top("القاهرة", AppLanguage.ARABIC).cityId)
    }

    @Test
    fun same_place_is_not_listed_twice() {
        val results = PlaceSearch.search("toshkent", AppLanguage.UZBEK, records)
        assertEquals(1, results.count { it.countryCode == "UZ" && Cities.distanceKm(it.latitude, it.longitude, 41.3, 69.24) < 15 })
    }

    @Test
    fun too_short_query_gives_nothing() {
        assertTrue(PlaceSearch.search("t", AppLanguage.UZBEK, records).isEmpty())
    }
}
