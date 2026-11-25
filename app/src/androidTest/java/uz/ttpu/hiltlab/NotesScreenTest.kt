package uz.ttpu.hiltlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.feature.notes.NotesScreen

class NotesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsNotesAndSendsClick() {
        var opened: Long? = null
        composeRule.setContent {
            NotesScreen(
                notes = listOf(Note(7, "Заметка A", "Текст")),
                onAdd = { _, _ -> },
                onDelete = {},
                onOpenNote = { opened = it },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Заметка A").assertIsDisplayed()
        composeRule.onNodeWithText("Заметка A").performClick()
        assertEquals(7L, opened)
    }
}
