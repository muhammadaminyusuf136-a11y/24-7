package uz.reja24.app.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import uz.reja24.app.service.ReceiverEntryPoint

/**
 * Telefon qayta yoqilganda, ilova yangilanganda, vaqt yoki vaqt zonasi o'zgarganda
 * barcha eslatmalar qaytadan hisoblanadi (aks holda ular yo'qolib qoladi).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ReceiverEntryPoint::class.java
        )
        entryPoint.applicationScope().launch {
            try {
                entryPoint.routineOrchestrator().syncAll()
            } catch (e: Exception) {
                Log.e(TAG, "Qayta rejalashtirishda xato", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
