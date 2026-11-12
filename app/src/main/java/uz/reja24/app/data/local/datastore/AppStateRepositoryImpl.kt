package uz.reja24.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.reja24.app.domain.model.TrophyLevel
import uz.reja24.app.domain.repository.AppStateRepository
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppStateRepository {

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val bypassUntilMillis: Flow<Long> =
        safeData.map { it[Keys.BYPASS_UNTIL] ?: 0L }.distinctUntilChanged()

    override suspend fun getBypassUntilMillis(): Long = bypassUntilMillis.first()

    override suspend fun setBypassUntilMillis(millis: Long) {
        dataStore.edit { it[Keys.BYPASS_UNTIL] = millis }
    }

    override val pendingTrophy: Flow<TrophyLevel?> = safeData
        .map { prefs ->
            prefs[Keys.PENDING_TROPHY]
                ?.let { name -> TrophyLevel.fromName(name) }
                ?.takeIf { it != TrophyLevel.NONE }
        }
        .distinctUntilChanged()

    override suspend fun setPendingTrophy(level: TrophyLevel?) {
        dataStore.edit { prefs ->
            if (level == null || level == TrophyLevel.NONE) {
                prefs.remove(Keys.PENDING_TROPHY)
            } else {
                prefs[Keys.PENDING_TROPHY] = level.name
            }
        }
    }

    override suspend fun getLastEvaluatedDate(): LocalDate? =
        safeData.first()[Keys.LAST_EVALUATED]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    override suspend fun setLastEvaluatedDate(date: LocalDate) {
        dataStore.edit { it[Keys.LAST_EVALUATED] = date.toString() }
    }

    private object Keys {
        val BYPASS_UNTIL = longPreferencesKey("bypass_until_millis")
        val PENDING_TROPHY = stringPreferencesKey("pending_trophy")
        val LAST_EVALUATED = stringPreferencesKey("last_evaluated_date")
    }
}
