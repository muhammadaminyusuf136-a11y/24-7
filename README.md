# 24/7 — «Har soating, rejangda»

Android ilova: kun tartibingizni yozasiz, belgilangan vaqtda chalg'ituvchi ilovalar
(Instagram, YouTube, TikTok, Telegram, brauzer) bloklanadi, ertalab o'zi ochiladi.
3 bosqichli eslatma, ball / yulduz / kubok tizimi. 4 til: O'zbek, English, Русский, العربية.

Ilova **to'liq offline** ishlaydi: server, ro'yxatdan o'tish, reklama yo'q.

---

## 1. Kompyuterga hech narsa o'rnatmasdan APK olish (tavsiya etiladi)

Kompyuteringiz kuchsiz bo'lsa ham bo'ladi — ilovani **GitHub'ning serverlari** yig'ib beradi.
Sizga faqat bepul GitHub akkaunti kerak.

### 1-qadam. GitHub akkaunti
1. Brauzerda **github.com** ga kiring → **Sign up** → elektron pochta, parol, foydalanuvchi nomi.
2. Pochtangizga kelgan kodni kiriting.

### 2-qadam. GitHub Desktop (loyihani yuklash dasturi, ~150 MB)
1. **desktop.github.com** → **Download for Windows** → o'rnating.
2. Dasturni oching → **Sign in to GitHub.com** → brauzerda **Authorize** bosing.

### 3-qadam. Loyihani GitHub'ga yuklash
1. GitHub Desktop'da: **File → Add local repository**.
2. **Choose...** → `D:\vebcoding\Reja24` papkasini tanlang.
3. «This directory does not appear to be a Git repository» deb chiqsa → **create a repository** havolasini bosing
   → **Create repository**.
4. Chapdagi pastki qismga (Summary) `Birinchi versiya` deb yozing → **Commit to main**.
5. Yuqorida **Publish repository** → **Keep this code private** belgisini qoldiring (yoki olib tashlang) → **Publish repository**.

### 4-qadam. APK tayyor bo'lishini kutish (5–10 daqiqa)
1. Brauzerda o'z loyihangizni oching: `github.com/<sizning-nomingiz>/Reja24`.
2. Yuqoridagi **Actions** bo'limiga o'ting.
3. «Build APK» degan qatorda sariq aylana — yig'ilmoqda. Yashil ✓ — tayyor. Qizil ✗ — xato (pastga qarang).

### 5-qadam. APK ni yuklab olish
1. Yashil ✓ bo'lgan qatorni bosing.
2. Pastda **Artifacts** bo'limida **24-7Reja-debug-apk** ni bosing → ZIP fayl yuklanadi.
3. ZIP ichidagi `app-debug.apk` faylini telefonga o'tkazing (Telegram «Saqlangan xabarlar», kabel yoki Google Drive orqali).

### 6-qadam. Telefonga o'rnatish
1. Telefonda APK faylni bosing.
2. «Noma'lum manbalardan o'rnatish» so'ralsa → **Sozlamalar** → shu ilova uchun ruxsat bering → orqaga qayting → **O'rnatish**.
3. Ilova «24/7» nomi bilan paydo bo'ladi (sinov nusxasi — paket nomi `uz.reja24.app.debug`).

> **Keyingi o'zgarishlar:** kodni o'zgartirgach GitHub Desktop'da yana **Commit to main** → **Push origin**.
> Actions avtomatik yangi APK yig'adi.

### Agar Actions'da qizil ✗ chiqsa
1. Qizil qatorni bosing → **Debug APK + testlar** → qizil qadamni oching.
2. Qizil matnning **oxirgi 30–40 qatorini** nusxalab Claude'ga yuboring — tuzatib beradi.
3. «Unit testlar» qadami qizil bo'lsa ham APK baribir yuklanadi (APK testlardan oldin yig'iladi).

---

## 2. Ilovadan birinchi marta foydalanish
1. Tanishuv (6 qadam): til → rejim vaqtlari → bloklanadigan ilovalar → ruxsatlar.
2. **3 ta muhim ruxsat:**
   - *Ilovalarni kuzatish* (Accessibility) — faqat qaysi ilova ochiqligini biladi.
   - *Ekran ustida ko'rsatish* — blok ekrani uchun.
   - *Batareya cheklovini olib tashlash* — telefon ilovani o'chirib qo'ymasligi uchun.
3. **Sozlama → «Bloklash ishonchli ishlayaptimi?»** — hammasi yashil ✓ bo'lishi kerak.
   Xiaomi, Samsung, Huawei, Oppo, Vivo, OnePlus uchun alohida qadamma-qadam yo'riqnoma bor.

**Sinab ko'rish uchun:** Sozlama → Rejim → «Bloklash boshlanadi» ni 2–3 daqiqa keyinga qo'ying,
«Uyg'onish» ni bir soat keyinga. Keyin bloklangan ilovani ochib ko'ring.

---

## 3. Play Market uchun imzolangan AAB (keyinroq)

Play Market'ga faqat **imzolangan AAB** yuklanadi. Imzo kaliti — ilovangizning «pasporti»,
uni **yo'qotmang** (yo'qolsa, ilovani yangilab bo'lmaydi).

### Kalit yaratish (bir marta, kompyuterga hech narsa o'rnatmasdan)
> Buni faqat repozitoriy **Private** (yopiq) bo'lsa qiling.

1. GitHub → loyihangiz → **Actions** → chapda **Imzo kalitini yaratish** → **Run workflow**.
2. Parol kiriting (kamida 8 belgi) va uni **qog'ozga yozib qo'ying** → yashil **Run workflow**.
3. 1–2 daqiqadan so'ng ishga tushgan qatorni oching → **Artifacts** → **reja24-imzo-kaliti** ni yuklab oling.
   Ichida ikki fayl bor:
   - `reja24-release.jks` — kalitning o'zi. **Xavfsiz joyda saqlang** (flesh, shaxsiy bulut). GitHub'ga yuklamang.
   - `KEYSTORE_BASE64.txt` — pastdagi Secret uchun matn.
4. Artefakt 1 kundan keyin GitHub'dan o'zi o'chadi.

### GitHub Secrets
Loyiha sahifasi → **Settings → Secrets and variables → Actions → New repository secret**:

| Nomi | Qiymati |
|---|---|
| `KEYSTORE_BASE64` | `KEYSTORE_BASE64.txt` faylidagi butun matn |
| `KEYSTORE_PASSWORD` | siz kiritgan parol |
| `KEY_ALIAS` | `reja24` |
| `KEY_PASSWORD` | o'sha parol |

### Reliz yig'ish
GitHub Desktop → **Repository → Create tag...** (yoki GitHub sahifasida **Releases → Draft a new release**)
→ teg nomi `v1.0.0`. Actions **24-7Reja-release** artefaktida `.aab` va imzolangan `.apk` beradi.

Keyingi versiyalarda `app/build.gradle.kts` dagi `versionCode` ni 1 ga oshiring va `versionName` ni yangilang.

---

## 4. Loyiha tuzilmasi (dasturchilar uchun)

```
app/src/main/java/uz/reja24/app/
├── core/            di (Hilt), util (vaqt, til, ruxsatlar, brendlar)
├── data/            Room (jadvallar, DAO), DataStore, repository implementatsiyalari
├── domain/          modellar, ball hisoblash, bloklash qoidasi, use case'lar (sof Kotlin)
├── presentation/    Compose ekranlar + ViewModel'lar (MVVM)
└── service/         AccessibilityService, overlay, Foreground Service, budilniklar, WorkManager
```

| Texnologiya | Qiymat |
|---|---|
| Til / UI | Kotlin, Jetpack Compose, Material 3 |
| Arxitektura | MVVM + Clean Architecture |
| Ma'lumot | Room (v1, sxema `app/schemas/`), DataStore |
| DI | Hilt |
| minSdk / targetSdk | 26 / 36 |
| Build | AGP 8.11, Gradle 8.14.3, Kotlin 2.1, JDK 17 |

**Buyruqlar** (Android Studio yoki JDK 17 + Android SDK bo'lsa):
- `gradlew assembleDebug` — sinov APK (`app/build/outputs/apk/debug/`)
- `gradlew testDebugUnitTest` — unit testlar
- `gradlew bundleRelease` — Play Market uchun AAB (imzo muhit o'zgaruvchilari bilan)

Qo'shimcha hujjatlar: [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md), [docs/PLAY_STORE.md](docs/PLAY_STORE.md).

## Litsenziyalar
Shriftlar (Space Grotesk, Manrope, Noto Sans Arabic) — SIL Open Font License 1.1, matnlari `licenses/` papkasida.
