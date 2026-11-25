package uz.ttpu.hiltlab.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.ttpu.hiltlab.feature.notes.NoteDetailRoute
import uz.ttpu.hiltlab.feature.notes.NotesRoute
import uz.ttpu.hiltlab.ui.users.UsersRoute

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenUsers = { navController.navigate("users") },
                onOpenNotes = { navController.navigate("notes") },
            )
        }
        composable("users") {
            UsersRoute(onBack = { navController.popBackStack() })
        }
        composable("notes") {
            NotesRoute(
                onOpenNote = { id -> navController.navigate("note/$id") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "note/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) {
            NoteDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
