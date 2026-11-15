package uz.reja24.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.reja24.app.data.local.db.dao.MuhosabaDao
import uz.reja24.app.data.local.db.entity.MuhosabaItemEntity
import uz.reja24.app.data.local.db.entity.MuhosabaMarkEntity
import uz.reja24.app.data.local.db.entity.TaskMissEntity
import uz.reja24.app.domain.repository.MuhosabaItem
import uz.reja24.app.domain.repository.MuhosabaRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MuhosabaRepositoryImpl @Inject constructor(
    private val dao: MuhosabaDao
) : MuhosabaRepository {

    override fun observeItems(): Flow<List<MuhosabaItem>> =
        dao.observeActiveItems().map { list -> list.map { MuhosabaItem(it.id, it.title) } }

    override fun observeMarks(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Map<Long, Boolean>>> =
        dao.observeMarks(from, to).map { marks ->
            marks.groupBy { it.date }.mapValues { (_, list) -> list.associate { it.itemId to it.done } }
        }

    override suspend fun addItem(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        dao.insertItem(MuhosabaItemEntity(title = clean, sortOrder = dao.maxSortOrder() + 1, isArchived = false))
    }

    override suspend fun removeItem(id: Long) = dao.archiveItem(id)

    override suspend fun setMark(itemId: Long, date: LocalDate, done: Boolean?) {
        if (done == null) dao.deleteMark(itemId, date) else dao.upsertMark(MuhosabaMarkEntity(itemId, date, done))
    }

    override fun observeTaskMisses(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Set<Long>>> =
        dao.observeTaskMisses(from, to).map { misses ->
            misses.groupBy { it.date }.mapValues { (_, list) -> list.mapTo(HashSet()) { it.entryId } }
        }

    override suspend fun setTaskMissed(entryId: Long, date: LocalDate, missed: Boolean) {
        if (missed) dao.upsertTaskMiss(TaskMissEntity(entryId, date)) else dao.deleteTaskMiss(entryId, date)
    }
}