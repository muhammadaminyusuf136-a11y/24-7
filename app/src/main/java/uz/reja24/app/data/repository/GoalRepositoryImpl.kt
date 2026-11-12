package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.GoalDao
import uz.reja24.app.data.mapper.toDomain
import uz.reja24.app.data.mapper.toEntity
import uz.reja24.app.domain.model.Goal
import uz.reja24.app.domain.repository.GoalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao
) : GoalRepository {

    override fun observeGoals(year: Int): Flow<List<Goal>> =
        dao.observeByYear(year).map { list -> list.map { it.toDomain() } }

    override suspend fun getGoal(id: Long): Goal? = dao.getById(id)?.toDomain()

    override suspend fun saveGoal(goal: Goal): Long {
        val result = dao.upsert(goal.toEntity())
        return if (goal.id == 0L) result else goal.id
    }

    override suspend fun deleteGoal(id: Long) = dao.deleteById(id)
}
