package uz.ttpu.hiltlab.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.ttpu.hiltlab.data.local.NoteDao
import uz.ttpu.hiltlab.data.local.NoteEntity
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.repository.RoomNoteRepository

/**
 * Q15: the repository takes a CoroutineDispatcher through a qualifier,
 * so the test passes a test dispatcher instead of Dispatchers.IO.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomNoteRepositoryTest {

    private class FakeNoteDao : NoteDao {
        private val rows = MutableStateFlow<List<NoteEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<NoteEntity>> = rows

        override suspend fun find(id: Long): NoteEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun upsert(note: NoteEntity): Long {
            val id = if (note.id == 0L) nextId++ else note.id
            rows.value = rows.value.filterNot { it.id == id } + note.copy(id = id)
            return id
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    private val repository = RoomNoteRepository(FakeNoteDao(), UnconfinedTestDispatcher())

    @Test
    fun saveAndGetRoundTrip() = runTest {
        val id = repository.save(Note(title = "Room", body = "Stored"))
        assertEquals("Room", repository.get(id)?.title)
    }

    @Test
    fun observeEmitsSavedNotes() = runTest {
        repository.save(Note(title = "A", body = ""))
        repository.save(Note(title = "B", body = ""))
        assertEquals(listOf("A", "B"), repository.observeNotes().first().map { it.title })
    }

    @Test
    fun deleteRemovesNote() = runTest {
        val id = repository.save(Note(title = "Temp", body = ""))
        repository.delete(id)
        assertEquals(null, repository.get(id))
    }
}
