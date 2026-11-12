package uz.reja24.app.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import uz.reja24.app.core.di.ApplicationScope
import uz.reja24.app.service.alarm.AlarmHandler

/** BroadcastReceiver'lar Hilt obyektlarini shu "eshik" orqali oladi. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun alarmHandler(): AlarmHandler
    fun routineOrchestrator(): RoutineOrchestrator

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
