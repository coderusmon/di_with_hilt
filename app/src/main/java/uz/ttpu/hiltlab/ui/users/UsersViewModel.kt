package uz.ttpu.hiltlab.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.ttpu.hiltlab.data.model.User
import uz.ttpu.hiltlab.data.repository.UserRepository

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<User>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UsersUiState>(UsersUiState.Loading)
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    init { load() }

    fun load(forceRefresh: Boolean = false) {
        _state.value = UsersUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                UsersUiState.Success(repository.users(forceRefresh))
            } catch (e: Exception) {
                UsersUiState.Error(e.message ?: "Network error")
            }
        }
    }
}
