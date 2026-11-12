package uz.reja24.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.reja24.app.data.local.db.entity.RoutineSettingsEntity

@Dao
interface RoutineSettingsDao {

    @Query("SELECT * FROM routine_settings WHERE id = 1")
    fun observe(): Flow<RoutineSettingsEntity?>

    @Query("SELECT * FROM routine_settings WHERE id = 1")
    suspend fun get(): RoutineSettingsEntity?

    @Upsert
    suspend fun upsert(settings: RoutineSettingsEntity)
}
