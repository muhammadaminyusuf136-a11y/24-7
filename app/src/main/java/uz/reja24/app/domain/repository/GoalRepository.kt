package uz.reja24.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.reja24.app.domain.model.Goal

interface GoalRepository {
    fun observeGoals(year: Int): Flow<List<Goal>>
    suspend fun getGoal(id: Long): Goal?
    suspend fun saveGoal(goal: Goal): Long
    suspend fun deleteGoal(id: Long)
}
