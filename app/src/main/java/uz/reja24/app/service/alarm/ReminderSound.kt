package uz.reja24.app.service.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import uz.reja24.app.domain.model.VoiceGender
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/** Eslatma ovozi: tanlangan rington va ovozli o'qish (TextToSpeech). */
object ReminderSound {

    private const val RINGTONE_PLAY_MS = 4_000L
    private const val TTS_TIMEOUT_MS = 15_000L

    /** Ovozli o'qish natijasi ("Eshitib ko'rish" tugmasi foydalanuvchiga aniq aytishi uchun). */
    enum class SpeakResult { SPOKEN, LANGUAGE_MISSING, ENGINE_MISSING }

    /** Tanlangan ringtonni bir necha soniya chaladi. */
    suspend fun playRingtone(context: Context, uriString: String?) {
        val uri: Uri = uriString?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val ringtone = try {
            RingtoneManager.getRingtone(context, uri)
        } catch (e: Exception) {
            null
        } ?: return
        try {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
            delay(RINGTONE_PLAY_MS)
        } catch (e: Exception) {
            // ovoz chalinmasa ham bildirishnoma ko'rsatilgan
        } finally {
            runCatching { ringtone.stop() }
        }
    }

    /**
     * Matnni ovoz chiqarib o'qiydi. Telefonda shu til uchun ovoz bo'lmasa
     * (masalan o'zbek tili ko'p telefonlarda yo'q) — o'qimaydi va buni natijada aytadi.
     */
    suspend fun speak(context: Context, text: String, locale: Locale, gender: VoiceGender): SpeakResult {
        val result = AtomicReference(SpeakResult.ENGINE_MISSING)
        withTimeoutOrNull(TTS_TIMEOUT_MS) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val finished = AtomicBoolean(false)
                var engine: TextToSpeech? = null

                fun finish() {
                    if (finished.compareAndSet(false, true)) {
                        runCatching { engine?.shutdown() }
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }

                engine = try {
                    TextToSpeech(context.applicationContext) { status ->
                        val tts = engine
                        if (status != TextToSpeech.SUCCESS || tts == null) {
                            finish()
                        } else {
                            val languageResult = tts.setLanguage(locale)
                            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                            ) {
                                result.set(SpeakResult.LANGUAGE_MISSING)
                                finish()
                            } else {
                                applyVoice(tts, locale, gender)
                                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                    override fun onStart(utteranceId: String?) = Unit
                                    override fun onDone(utteranceId: String?) = finish()

                                    @Deprecated("Deprecated in Java")
                                    override fun onError(utteranceId: String?) = finish()
                                    override fun onError(utteranceId: String?, errorCode: Int) = finish()
                                })
                                val spoken = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reja24_reminder")
                                if (spoken == TextToSpeech.SUCCESS) result.set(SpeakResult.SPOKEN) else finish()
                            }
                        }
                    }
                } catch (e: Exception) {
                    null
                }
                if (engine == null) finish()
                continuation.invokeOnCancellation { finish() }
            }
        }
        return result.get()
    }

    /**
     * Android ovozning jinsini API orqali bermaydi. Nomida "female"/"male" yoki Samsung'ning
     * "SMTf00"/"SMTm00" belgisi bo'lsa — shu ovoz tanlanadi. Topilmasa, standart ovoz
     * ohangi (pitch) o'zgartiriladi: ayol — balandroq, erkak — pastroq. Farq har doim eshitiladi.
     */
    private fun applyVoice(tts: TextToSpeech, locale: Locale, gender: VoiceGender) {
        val candidates = runCatching { tts.voices }.getOrNull().orEmpty().filter { voice ->
            voice.locale.language == locale.language &&
                !voice.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
        }
        val match = candidates.firstOrNull { genderOf(it) == gender }
        if (match != null && runCatching { tts.setVoice(match) }.getOrNull() == TextToSpeech.SUCCESS) {
            tts.setPitch(1.0f)
        } else {
            tts.setPitch(if (gender == VoiceGender.FEMALE) FEMALE_PITCH else MALE_PITCH)
        }
    }

    private fun genderOf(voice: Voice): VoiceGender? {
        val name = voice.name.lowercase(Locale.ROOT)
        return when {
            "female" in name -> VoiceGender.FEMALE
            "male" in name -> VoiceGender.MALE
            else -> SAMSUNG_PATTERN.find(name)?.groupValues?.get(1)?.let {
                if (it == "f") VoiceGender.FEMALE else VoiceGender.MALE
            }
        }
    }

    private val SAMSUNG_PATTERN = Regex("smt([fm])\\d")
    private const val FEMALE_PITCH = 1.2f
    private const val MALE_PITCH = 0.78f
}
