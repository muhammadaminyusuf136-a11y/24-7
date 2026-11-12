package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.BlockedAppDao
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.BlockedApp
import uz.reja24.app.domain.repository.BlockedAppRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedAppRepositoryImpl @Inject constructor(
    private val dao: BlockedAppDao
) : BlockedAppRepository {

    override fun observeAll(): Flow<List<BlockedApp>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBlockedPackages(): Flow<Set<String>> =
        dao.observeBlockedPackageNames().map { it.toSet() }.distinctUntilChanged()

    override suspend fun getBlockedPackages(): Set<String> = dao.getBlockedPackageNames().toSet()

    override suspend fun saveApp(app: BlockedApp) = dao.upsert(app.toEntity())

    override suspend fun saveApps(apps: List<BlockedApp>) = dao.upsertAll(apps.map { it.toEntity() })

    override suspend fun setBlocked(packageName: String, isBlocked: Boolean) =
        dao.setBlocked(packageName, isBlocked)

    override suspend fun removeApp(packageName: String) = dao.delete(packageName)
}
