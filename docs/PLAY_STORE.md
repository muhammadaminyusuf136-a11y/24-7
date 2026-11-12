# Google Play'ga nashr qilish — tayyor javoblar va materiallar

Bu fayl Play Console'dagi shakllarni to'ldirishda **nusxalab qo'yish** uchun.

---

## 1. Maxfiylik siyosati havolasi (MAJBURIY)
Play Console ochiq (public) URL so'raydi. Eng oson yo'l:
1. **gist.github.com** → `docs/PRIVACY_POLICY.md` matnini joylang → **Create public gist**.
2. Gist havolasini Play Console → **App content → Privacy policy** ga qo'ying.

(Alternativa: Google Sites'da bepul sahifa.)

---

## 2. Accessibility API deklaratsiyasi (Play Console → App content → Accessibility API)

**Ilova AccessibilityService'ni nima uchun ishlatadi (inglizcha, nusxalash uchun):**

> 24/7 is a digital-wellbeing and bedtime-routine app. The user creates their own daily routine and
> chooses which distracting apps (e.g. Instagram, YouTube, TikTok) should be unavailable during their
> self-defined bedtime window. The AccessibilityService listens only for TYPE_WINDOW_STATE_CHANGED events
> to learn the package name of the app that has just come to the foreground. If that package is on the
> user's own block list and the current time is inside the user's routine window, the app returns the
> user to the home screen and shows a block screen with the unlock time.
>
> The service does NOT retrieve window content (canRetrieveWindowContent="false"), does not read text,
> input, passwords or messages, does not perform actions on other apps' UI, and does not collect,
> store or transmit any data. The app has no INTERNET permission. isAccessibilityTool="false".
> Calls, SMS, emergency apps, clock and system settings are never blocked.

**Prominent Disclosure** — ilova ichida allaqachon bor: Accessibility sozlamasini ochishdan oldin
«Ilovalarni kuzatish nima uchun kerak?» oynasi chiqadi (`AccessibilityDisclosureDialog`):
- nima uchun kerakligi,
- faqat ilova nomi o'qilishi, yozuvlar/parollar/xabarlar o'qilmasligi,
- «Tushundim, davom etish» / «Hozir emas» tugmalari (majburlash yo'q).

Google ko'pincha **video** so'raydi: telefon ekranini yozib oling (tanishuv → ruxsatlar → oynani ko'rsatish →
Accessibility yoqish → Instagram ochilganda blok ekrani). 30–60 soniya yetarli, YouTube'ga «Unlisted» yuklab havolani bering.

---

## 3. Boshqa ruxsatlar uchun asoslash

| Ruxsat | Asoslash (inglizcha) |
|---|---|
| `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the user's self-configured bedtime blocking routine reliably active; shows an ongoing notification with the block window. No data processing. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Core function: app blocking and bedtime reminders must fire at exact times; OEM battery managers otherwise kill the service. User is asked explicitly and can decline. |
| `SCHEDULE_EXACT_ALARM` | Staged bedtime reminders (60/30/15/1 minutes before) and plan reminders chosen by the user. Falls back to inexact alarms if denied. |
| `SYSTEM_ALERT_WINDOW` | Displays the block screen over a blocked app during the routine window. |
| `RECEIVE_BOOT_COMPLETED` | Restores the user's reminders after reboot. |

`USE_EXACT_ALARM` va `QUERY_ALL_PACKAGES` **ishlatilmaydi** (Play siyosati bo'yicha cheklangan ruxsatlar).

---

## 4. Data safety shakli — javoblar

| Savol | Javob |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data leaves the device) |
| Do you provide a way for users to request that their data is deleted? | Data is stored only on device; uninstall / Clear data deletes it |
| Ads | **No ads** |
| Target audience | 13+ (tavsiya: 16+) |
| Content rating (IARC) | Barcha savollarga «No» → odatda **3+ / Everyone** |

Izoh: ilova ma'lumotni faqat qurilmada qayta ishlaydi va internetga ulanmaydi — Google ta'rifi bo'yicha bu «collection» hisoblanmaydi.

---

## 5. Ikonka va do'kon materiallari

| Material | O'lcham / talab |
|---|---|
| Ilova ikonkasi (Play) | **512 × 512 px**, PNG, 32-bit, ≤ 1 MB, burchaklarsiz (Play o'zi yumaloqlaydi) |
| Feature graphic | **1024 × 500 px**, PNG/JPG, matn chetdan uzoqda |
| Telefon skrinshotlari | kamida **2 ta** (tavsiya 4–8), 16:9 yoki 9:16, qisqa tomoni ≥ 1080 px |
| Planshet skrinshotlari (ixtiyoriy) | 7" va 10" uchun, ≥ 1080 px |
| Qisqa tavsif | ≤ 80 belgi |
| To'liq tavsif | ≤ 4000 belgi |

**Ikonka dizayni:** ilovadagi adaptive icon bilan bir xil — loy rangi fon `#D9663D`, oq 24 soatlik doira va soat millari.
512 px versiyani Figma/Canva'da shu ranglar bilan chizing yoki Claude'dan SVG so'rang.

**Skrinshotlar uchun ekranlar:** Bosh sahifa (surib ko'riladigan lenta), Haftalik jadval, Kunlik reja, Blok ekrani, Natija (kuboklar), Sozlamalar (4 til).

**Qisqa tavsif (uz):** Kun tartibingizni rejalang, kechasi chalg'ituvchi ilovalarni bloklang, vaqtida uxlang.
**Short description (en):** Plan your day, block distracting apps at night, and get to bed on time.

---

## 6. Nashrdan oldin tekshiruv ro'yxati
- [ ] `versionCode` / `versionName` yangilangan
- [ ] Imzolangan AAB (`v1.0.0` teg → Actions → 24-7Reja-release)
- [ ] Maxfiylik siyosati URL
- [ ] Accessibility deklaratsiyasi + video
- [ ] Data safety to'ldirilgan
- [ ] Content rating anketasi
- [ ] Ikonka 512, feature graphic, 4+ skrinshot
- [ ] Avval **Internal testing** trekiga yuklab, o'z telefoningizda sinab ko'ring
