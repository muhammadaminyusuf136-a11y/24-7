package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.BlockedAppEntity

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps ORDER BY appName COLLATE NOCASE")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1")
    fun observeBlockedPackageNames(): Flow<List<String>>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getBlockedPackageNames(): List<String>

    @Upsert
    suspend fun upsert(app: BlockedAppEntity)

    @Upsert
    suspend fun upsertAll(apps: List<BlockedAppEntity>)

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun setBlocked(packageName: String, isBlocked: Boolean)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
