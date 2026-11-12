package uz.reja24.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.reja24.app.R

/*
 * Shriftlar — "variable font" (o'zgaruvchan qalinlikdagi) fayllar.
 * Bitta fayl ichida barcha qalinliklar bor, shuning uchun ilova hajmi kichik.
 * Fayllar res/font papkasida, litsenziyasi: SIL Open Font License (licenses/ papkasi).
 *
 * Eslatma: Space Grotesk'da kirill harflari yo'q — rus tilida sarlavhalardagi
 * kirill harflari Android tomonidan avtomatik tizim shriftida chiziladi.
 */

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Sarlavha, raqamlar, soat: Space Grotesk (400/500/700) */
val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal),
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.Bold)
)

/** Asosiy matn: Manrope (400/500/600/700) */
val Manrope = FontFamily(
    variableFont(R.font.manrope, FontWeight.Normal),
    variableFont(R.font.manrope, FontWeight.Medium),
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold)
)

/** Arab tili: Noto Sans Arabic */
val NotoSansArabic = FontFamily(
    variableFont(R.font.noto_sans_arabic, FontWeight.Normal),
    variableFont(R.font.noto_sans_arabic, FontWeight.Medium),
    variableFont(R.font.noto_sans_arabic, FontWeight.SemiBold),
    variableFont(R.font.noto_sans_arabic, FontWeight.Bold)
)

/**
 * Tilga qarab typography yaratadi.
 * Arab tilida hamma matn Noto Sans Arabic bilan, boshqa tillarda Space Grotesk + Manrope.
 */
fun rejaTypography(isArabic: Boolean): Typography {
    val heading = if (isArabic) NotoSansArabic else SpaceGrotesk
    val body = if (isArabic) NotoSansArabic else Manrope
    return Typography(
        // Katta soat, ballar
        displayLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 52.sp),
        displayMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 46.sp),
        displaySmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 38.sp),
        // Sarlavhalar
        headlineLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 23.sp, lineHeight = 29.sp),
        titleLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        // Asosiy matn
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        // Tugmalar, chiplar
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp)
    )
}
