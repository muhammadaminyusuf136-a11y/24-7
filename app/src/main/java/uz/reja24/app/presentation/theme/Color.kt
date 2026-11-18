package uz.reja24.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Ilovada 4 ta rang bor (mavzu va fon bitta tanlov): Qaymoq, To'q ko'k, To'q yashil, To'q pushti.
// Sof oq va oqqa yaqin ranglar ishlatilmaydi — ko'zni charchatadi.

// ---------- "Qaymoq" — kulrang-qaymoq, yorug' (o'g'il va qizlarga mos) ----------
// Kontrast fon #E7E1D5 ustida: textPrimary ≈ 14:1, textMuted ≈ 4.8:1, accentText ≈ 4.8:1
val LightBackground = Color(0xFFE7E1D5)
val LightSurface = Color(0xFFEFE9DE)         // kartalar — fondan biroz ochroq, lekin oq emas
val LightBorder = Color(0xFFD3CBBA)
val LightTextPrimary = Color(0xFF1E1F22)
val LightTextSecondary = Color(0xFF4E4A42)
val LightTextMuted = Color(0xFF66604F)
val LightAccent = Color(0xFFC7643C)          // loy rangi
val LightAccentText = Color(0xFF9C4724)
val LightTimelineEmpty = Color(0xFFDDD5C5)
val LightHijri = Color(0xFF1F6B50)           // hijriy sana — yashil, fon ustida ≈ 4.9:1

// ---------- "To'q ko'k" ----------
// Kontrast fon #0F1D30 ustida: textPrimary ≈ 15:1, textSecondary ≈ 9:1, textMuted ≈ 5.8:1
val NavyBackground = Color(0xFF0F1D30)
val NavySurface = Color(0xFF172A42)
val NavyBorder = Color(0xFF263B5A)
val NavyTextPrimary = Color(0xFFEEF2F8)
val NavyTextSecondary = Color(0xFFAEBBD0)
val NavyTextMuted = Color(0xFF8C9BB3)
val NavyAccent = Color(0xFFF08A3C)            // to'q sariq — ko'k ustida yaxshi ko'rinadi
val NavyAccentText = Color(0xFFF4A15F)
val NavyTimelineEmpty = Color(0xFF22364F)
val NavyHijri = Color(0xFF8ED9B5)

// ---------- "To'q yashil" ----------
// Kontrast fon #0F2219 ustida: textPrimary ≈ 15:1, textMuted ≈ 6.5:1
val GreenBackground = Color(0xFF0F2219)
val GreenSurface = Color(0xFF173025)
val GreenBorder = Color(0xFF254536)
val GreenTextPrimary = Color(0xFFEDF4EF)
val GreenTextSecondary = Color(0xFFAFC6BA)
val GreenTextMuted = Color(0xFF8DA89B)
val GreenAccent = Color(0xFFE39A45)           // qahrabo — yashil ustida ajralib turadi
val GreenAccentText = Color(0xFFF0B46C)
val GreenTimelineEmpty = Color(0xFF1F3A2D)
val GreenHijri = Color(0xFFEBC77A)            // yashil fonda hijriy sana oltin rangda

// ---------- "To'q pushti" ----------
// Kontrast fon #2A1220 ustida: textPrimary ≈ 15:1, textMuted ≈ 6.6:1
val GraceBackground = Color(0xFF2A1220)
val GraceSurface = Color(0xFF3A1A2C)
val GraceBorder = Color(0xFF55293F)
val GraceTextPrimary = Color(0xFFF8ECF1)
val GraceTextSecondary = Color(0xFFE0B9C8)
val GraceTextMuted = Color(0xFFBF95A7)
val GraceAccent = Color(0xFFF06C92)           // atirgul
val GraceAccentText = Color(0xFFF796B2)
val GraceTimelineEmpty = Color(0xFF47213A)
val GraceHijri = Color(0xFF8ED9B5)

// ---------- Toifa ranglari (kunlik reja turlari) ----------
val CategoryWork = Color(0xFFD9663D)      // ish
val CategoryStudy = Color(0xFF3E6B5B)     // dars
val CategoryFamily = Color(0xFF7B8F5A)    // oila
val CategoryReading = Color(0xFF6B6191)   // mutolaa
val CategorySleep = Color(0xFF46596B)     // uyqu
val CategoryMorning = Color(0xFFE0A44A)   // tong
val CategoryOther = Color(0xFFB5AEA1)     // boshqa

// ---------- Namoz ----------
val PrayerGreen = Color(0xFF2B7A5E)       // oq matn bilan ≈ 5.4:1

// ---------- Umumiy ----------
val InkBlack = Color(0xFF16181C)          // "Keyingi" kartasi foni
val SuccessGreen = Color(0xFF3E8E5B)
val MissedRed = Color(0xFFB3261E)        // "bajarilmadi" ✗ — oq belgi bilan ≈ 6.5:1
val MissedRedOnDark = Color(0xFFFF8A7A)  // to'q fonda "bajarilmadi" yozuvi uchun ochroq qizil
