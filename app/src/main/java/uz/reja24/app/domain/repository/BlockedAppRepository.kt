package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.BlockedApp

interface BlockedAppRepository {
    fun observeAll(): Flow<List<BlockedApp>>

    /** Hozir bloklanishi kerak bo'lgan paketlar nomlari (bloklash xizmati uchun tezkor). */
    fun observeBlockedPackages(): Flow<Set<String>>
    suspend fun getBlockedPackages(): Set<String>

    suspend fun saveApp(app: BlockedApp)
    suspend fun saveApps(apps: List<BlockedApp>)
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)
    suspend fun removeApp(packageName: String)
}
