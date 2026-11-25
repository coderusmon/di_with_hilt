package uz.ttpu.hiltlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.repository.NoteRepository
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NoteRepositoryHiltTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: NoteRepository

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun inMemoryRoomStoresNote() = runBlocking {
        assertTrue(repository.observeNotes().first().isEmpty())
        val id = repository.save(Note(title = "From test", body = "in-memory"))
        assertEquals("From test", repository.get(id)?.title)
    }
}
