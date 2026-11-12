package uz.reja24.app.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import uz.reja24.app.service.ReceiverEntryPoint

/** Budilnik chalinganda Android shu klassni chaqiradi. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync — ma'lumot bazasidan o'qish uchun bir necha soniya vaqt so'raymiz
        val pending = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, ReceiverEntryPoint::class.java
        )
        entryPoint.applicationScope().launch {
            try {
                entryPoint.alarmHandler().handle(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Eslatmani ko'rsatishda xato", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
