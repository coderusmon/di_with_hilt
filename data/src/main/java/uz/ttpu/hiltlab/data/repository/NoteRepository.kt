package uz.ttpu.hiltlab.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import uz.ttpu.hiltlab.data.di.IoDispatcher
import uz.ttpu.hiltlab.data.local.NoteDao
import uz.ttpu.hiltlab.data.local.toEntity
import uz.ttpu.hiltlab.data.local.toModel
import uz.ttpu.hiltlab.data.model.Note

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun get(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long)
}

@Singleton
class RoomNoteRepository @Inject constructor(
    private val dao: NoteDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }.flowOn(io)

    override suspend fun get(id: Long): Note? = withContext(io) {
        dao.find(id)?.toModel()
    }

    override suspend fun save(note: Note): Long = withContext(io) {
        dao.upsert(note.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(io) {
        dao.delete(id)
    }
}

/** Fake implementation used by the `-PuseFakeNotes=true` build and by UI demos. */
@Singleton
class FakeNoteRepository @Inject constructor() : NoteRepository {

    private val notes = MutableStateFlow(
        listOf(
            Note(1, "Добро пожаловать", "Это заметка из FakeNoteRepository (build без Room).", 1),
            Note(2, "Hilt", "Зависимости приходят через конструктор.", 2),
        )
    )

    override fun observeNotes(): Flow<List<Note>> = notes

    override suspend fun get(id: Long): Note? = notes.value.firstOrNull { it.id == id }

    override suspend fun save(note: Note): Long {
        val id = if (note.id == 0L) (notes.value.maxOfOrNull { it.id } ?: 0L) + 1 else note.id
        notes.update { current ->
            current.filterNot { it.id == id } + note.copy(id = id)
        }
        return id
    }

    override suspend fun delete(id: Long) {
        notes.update { current -> current.filterNot { it.id == id } }
    }
}
