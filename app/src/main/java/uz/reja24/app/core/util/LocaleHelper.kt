package uz.reja24.app.core.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import uz.reja24.app.domain.model.AppLanguage
import java.util.Locale

/**
 * Til bilan ishlash.
 * Ekranlar AppCompatDelegate orqali darhol yangi tilga o'tadi (restart kerak emas).
 * Xizmatlar va bildirishnomalar esa (Android 12 va pastda) tizim tilida qolmasligi uchun
 * `localizedContext` ishlatiladi.
 */
object LocaleHelper {

    /** Tilni butun ilovaga qo'llaydi. Activity avtomatik qayta chiziladi. */
    fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /** Hozir ilovada qaysi til faol (tanlanmagan bo'lsa — telefon tili yoki o'zbekcha). */
    fun currentLanguage(context: Context): AppLanguage {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) {
            appLocales[0]?.language
        } else {
            context.resources.configuration.locales[0]?.language
        }
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.UZBEK
    }

    /** Berilgan tilda resurslarni beradigan kontekst (xizmatlar, bildirishnomalar uchun). */
    fun localizedContext(context: Context, language: AppLanguage?): Context {
        if (language == null) return context
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
        return context.createConfigurationContext(config)
    }

    fun locale(language: AppLanguage): Locale = Locale.forLanguageTag(language.tag)
}
