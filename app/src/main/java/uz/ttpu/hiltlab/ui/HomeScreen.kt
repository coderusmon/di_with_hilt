package uz.ttpu.hiltlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenUsers: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("HiltLab") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Dependency Injection with Hilt",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Зависимости приходят через конструктор, модули Hilt собирают граф, " +
                    "Room кеширует пользователей, заметки редактируются через общий репозиторий.",
                style = MaterialTheme.typography.bodyMedium,
            )

            FeatureCard(
                title = "GitHub users",
                subtitle = "Retrofit + Room cache + @HiltViewModel",
                onClick = onOpenUsers,
            )
            FeatureCard(
                title = "Notes",
                subtitle = "Room / FakeNoteRepository, SavedStateHandle, @ViewModelScoped",
                onClick = onOpenNotes,
            )
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
