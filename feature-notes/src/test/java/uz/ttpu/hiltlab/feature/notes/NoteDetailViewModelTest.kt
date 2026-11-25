package uz.ttpu.hiltlab.feature.notes

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.ttpu.hiltlab.data.repository.FakeNoteRepository
import uz.ttpu.hiltlab.data.scopes.DateFormatter

/** Q18: the note id comes from SavedStateHandle, not from a constructor parameter. */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()

    private fun viewModel(id: Long) = NoteDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("noteId" to id)),
        repository = repository,
        formatter = DateFormatter(),
    )

    @Test
    fun loadsNoteFromSavedStateHandle() = runTest {
        val vm = viewModel(1)
        advanceUntilIdle()

        assertEquals("Добро пожаловать", vm.state.value.title)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun saveWritesEditsToRepository() = runTest {
        val vm = viewModel(1)
        advanceUntilIdle()

        vm.onTitleChange("Обновлено")
        vm.onBodyChange("Новый текст")
        vm.save()
        advanceUntilIdle()

        assertEquals("Обновлено", repository.get(1)?.title)
        assertEquals("Новый текст", repository.get(1)?.body)
        assertTrue(vm.state.value.saved)
        assertTrue(vm.state.value.formattedDate.isNotBlank())
    }

    @Test
    fun missingNoteLeavesEmptyState() = runTest {
        val vm = viewModel(99)
        advanceUntilIdle()

        assertEquals("", vm.state.value.title)
    }
}
