package uz.ttpu.hiltlab.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.repository.NoteRepository
import uz.ttpu.hiltlab.data.scopes.DateFormatter

data class NoteDetailState(
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = 0L,
    val formattedDate: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val formatter: DateFormatter,
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L

    private val _state = MutableStateFlow(NoteDetailState())
    val state: StateFlow<NoteDetailState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val note = repository.get(noteId) ?: return@launch
            _state.value = NoteDetailState(
                title = note.title,
                body = note.body,
                updatedAt = note.updatedAt,
                formattedDate = formatter.format(note.updatedAt),
            )
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, saved = false) }

    fun onBodyChange(value: String) = _state.update { it.copy(body = value, saved = false) }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            repository.save(
                Note(
                    id = noteId,
                    title = current.title.trim(),
                    body = current.body.trim(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            _state.update { it.copy(saved = true, formattedDate = formatter.format(System.currentTimeMillis())) }
        }
    }
}
