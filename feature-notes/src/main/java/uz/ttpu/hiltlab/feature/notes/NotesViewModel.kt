package uz.ttpu.hiltlab.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.repository.NoteRepository
import uz.ttpu.hiltlab.data.scopes.ViewModelTracker

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    tracker: ViewModelTracker,
) : ViewModel() {

    init { println("NotesViewModel uses ViewModelTracker #${tracker.number}") }

    val notes: StateFlow<List<Note>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String, body: String) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            repository.save(Note(title = title.trim(), body = body.trim()))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
