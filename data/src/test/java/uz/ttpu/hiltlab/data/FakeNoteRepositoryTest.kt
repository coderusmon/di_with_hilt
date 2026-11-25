package uz.ttpu.hiltlab.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.repository.FakeNoteRepository

@OptIn(ExperimentalCoroutinesApi::class)
class FakeNoteRepositoryTest {

    private val repository = FakeNoteRepository()

    @Test
    fun startsWithTwoDemoNotes() = runTest {
        assertEquals(2, repository.observeNotes().first().size)
    }

    @Test
    fun saveAssignsNextId() = runTest {
        val id = repository.save(Note(title = "New", body = "Body"))
        assertEquals(3L, id)
        assertNotNull(repository.get(id))
    }

    @Test
    fun saveUpdatesExistingNote() = runTest {
        val original = repository.get(1)!!
        repository.save(original.copy(title = "Changed"))
        assertEquals("Changed", repository.get(1)?.title)
        assertEquals(2, repository.observeNotes().first().size)
    }

    @Test
    fun deleteRemovesOnlyThatNote() = runTest {
        repository.delete(1)
        assertNull(repository.get(1))
        assertEquals(1, repository.observeNotes().first().size)
    }
}
