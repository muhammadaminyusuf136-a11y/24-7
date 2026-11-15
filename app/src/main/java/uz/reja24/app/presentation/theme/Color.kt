package uz.reja24.app.presentation.theme

import androidx.compose.ui.graphics.Color

// ---------- Oq mavzu (Light) = "Qaymoq" foni ----------
// Sof oq (#FFFFFF) ishlatilmaydi — ko'zni charchatadi. Fon — qaymoq, kartalar — sut rang.
val LightBackground = Color(0xFFF5EDDC)      // qaymoq
val LightSurface = Color(0xFFFCF8EF)         // sut rang (kartalar)
val LightBorder = Color(0xFFE4D8C1)
val LightTextPrimary = Color(0xFF16181C)
val LightTextSecondary = Color(0xFF57534A)
val LightTextMuted = Color(0xFF736A5B)       // fon ustida ≈ 4.8:1
val LightAccent = Color(0xFFD9663D)          // loy rangi
val LightAccentText = Color(0xFFA84B28)      // matn uchun, fon ustida ≈ 5:1
val LightTimelineEmpty = Color(0xFFEBE1CC)

// ---------- "Pushti" foni (avvalgi "Nafosat") ----------
// Butun ilova bitta tekis pushti rangda — gradient yo'q, shuning uchun hamma ekranda bir xil.
// Kontrast (WCAG AA) fon #F7E1E5 ustida:
//   textPrimary #2D171D ≈ 14:1, textSecondary #5F3E47 ≈ 7.5:1,
//   textMuted #7A5560 ≈ 5.2:1, accentText #A12B48 ≈ 5.6:1
val GraceBackgroundTop = Color(0xFFF7E1E5)    // pushti
val GraceSurface = Color(0xFFFDF1F3)          // och pushti (kartalar)
val GraceBorder = Color(0xFFEBC8D0)
val GraceTextPrimary = Color(0xFF2D171D)
val GraceTextSecondary = Color(0xFF5F3E47)
val GraceTextMuted = Color(0xFF7A5560)
val GraceAccent = Color(0xFFC73A5A)           // qizil-atirgul
val GraceAccentText = Color(0xFFA12B48)
val GraceTimelineEmpty = Color(0xFFF0CFD6)

// ---------- "To'q ko'k" foni (avvalgi "Kuch") ----------
// Kartalar, menyu va chetlar ham ko'k — avval qora edi va fonga mos tushmasdi.
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
