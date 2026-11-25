package uz.ttpu.hiltlab.feature.notes

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import uz.ttpu.hiltlab.data.repository.FakeNoteRepository
import uz.ttpu.hiltlab.data.scopes.ViewModelTracker

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()

    private fun viewModel() = NotesViewModel(repository, ViewModelTracker())

    @Test
    fun addInsertsNote() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notes.collect { } }

        vm.add("  Покупки  ", "  Молоко  ")
        advanceUntilIdle()

        val added = vm.notes.value.last()
        assertEquals("Покупки", added.title)
        assertEquals("Молоко", added.body)
        assertEquals(3, vm.notes.value.size)
    }

    @Test
    fun blankNoteIsIgnored() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notes.collect { } }

        vm.add("   ", "  ")
        advanceUntilIdle()

        assertEquals(2, vm.notes.value.size)
    }

    @Test
    fun deleteRemovesNote() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notes.collect { } }

        vm.delete(1)
        advanceUntilIdle()

        assertEquals(1, vm.notes.value.size)
        assertNull(repository.get(1))
    }
}
