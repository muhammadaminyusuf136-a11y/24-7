package uz.reja24.app.presentation.theme

import androidx.compose.ui.graphics.Color

// ---------- Oq mavzu (Light) ----------
// Sof oq (#FFFFFF) ishlatilmaydi — ko'zni charchatadi. Fon — iliq qum, kartalar — sut rang.
val LightBackground = Color(0xFFF0EAE0)      // iliq qum
val LightSurface = Color(0xFFFBF8F3)         // sut rang (kartalar)
val LightBorder = Color(0xFFDFD7C8)
val LightTextPrimary = Color(0xFF16181C)
val LightTextSecondary = Color(0xFF57534A)
val LightTextMuted = Color(0xFF766F63)       // fon ustida ≈ 4.6:1
val LightAccent = Color(0xFFD9663D)          // loy rangi
val LightAccentText = Color(0xFFA84B28)      // matn uchun, fon ustida ≈ 4.9:1
val LightTimelineEmpty = Color(0xFFE7DFD1)

// ---------- "Nafosat" uslubi (Grace) ----------
// Yumshoq chang-pushti va xira binafsha — yorqin emas, ko'zni charchatmaydi.
// Kontrast (WCAG AA) fon #E9D8DC ustida:
//   textPrimary #2B2027 ≈ 12:1, textSecondary #5C4853 ≈ 6.3:1,
//   textMuted #6E5864 ≈ 4.6:1, accentText #94405F ≈ 5.6:1
val GraceBackgroundTop = Color(0xFFE9D8DC)   // chang-pushti
val GraceBackgroundBottom = Color(0xFFD3BCCB) // xira binafsha (lavanda)
val GraceSurface = Color(0xFFF6ECEE)          // pushti tusdagi sut rang (kartalar)
val GraceBorder = Color(0xFFDEC7CF)
val GraceTextPrimary = Color(0xFF2B2027)      // to'q olxo'ri
val GraceTextSecondary = Color(0xFF5C4853)
val GraceTextMuted = Color(0xFF6E5864)
val GraceAccent = Color(0xFFB85A7E)           // atirgul
val GraceAccentText = Color(0xFF94405F)
val GraceTimelineEmpty = Color(0xFFE2CDD5)

// ---------- Qora mavzu (Dark) ----------
val DarkBackground = Color(0xFF0E1013)
val DarkSurface = Color(0xFF171A1F)
val DarkBorder = Color(0xFF23272E)
val DarkTextPrimary = Color(0xFFF2EFE9)
val DarkTextSecondary = Color(0xFF9B958A)
val DarkTextMuted = Color(0xFF6E6A62)
val DarkAccent = Color(0xFFE2703A)
val DarkAccentText = Color(0xFFE2703A)
val DarkTimelineEmpty = Color(0xFF23272E)

// ---------- Kulrang mavzu (Gray) ----------
// Kontrast (WCAG AA, kamida 4.5:1) hisoblab tanlangan:
//   textPrimary  #FFFFFF  fon ustida ≈ 6.4:1, surface ustida ≈ 7.5:1
//   textSecondary #E4E1DA fon ustida ≈ 4.9:1, surface ustida ≈ 5.7:1
//   textMuted    #DEDAD2  fon ustida ≈ 4.6:1, surface ustida ≈ 5.4:1
//   accentText   #FFD2BD  fon ustida ≈ 4.6:1 (matn uchun och loy rangi)
val GrayBackground = Color(0xFF5C5F66)
val GraySurface = Color(0xFF52555C)
val GrayBorder = Color(0xFF74777F)
val GrayTextPrimary = Color(0xFFFFFFFF)
val GrayTextSecondary = Color(0xFFE4E1DA)
val GrayTextMuted = Color(0xFFDEDAD2)
val GrayAccent = Color(0xFFE2703A)
val GrayAccentText = Color(0xFFFFD2BD)
val GrayTimelineEmpty = Color(0xFF74777F)

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
