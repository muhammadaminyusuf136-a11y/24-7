package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.StreakDao
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.StreakCalculator
import uz.reja24.app.domain.model.StreakRecord
import uz.reja24.app.domain.model.StreakSummary
import uz.reja24.app.domain.repository.StreakRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepositoryImpl @Inject constructor(
    private val dao: StreakDao
) : StreakRepository {

    override fun observeSummary(today: LocalDate): Flow<StreakSummary> =
        dao.observeAll().map { list -> StreakCalculator.calculate(list.map { it.toDomain() }, today) }

    override suspend fun getSummary(today: LocalDate): StreakSummary =
        StreakCalculator.calculate(dao.getAll().map { it.toDomain() }, today)

    override suspend fun saveRecord(record: StreakRecord) = dao.upsert(record.toEntity())
}
