package com.nextgen.player.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nextgen.player.library.ui.FolderScreen
import com.nextgen.player.library.ui.LibraryScreen
import com.nextgen.player.ui.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LIBRARY = "library"
    const val FOLDER = "folder/{folderPath}/{folderName}"
    const val SETTINGS = "settings"

    fun folderRoute(folderPath: String, folderName: String): String {
        val encodedPath = URLEncoder.encode(folderPath, "UTF-8")
        val encodedName = URLEncoder.encode(folderName, "UTF-8")
        return "folder/$encodedPath/$encodedName"
    }
}

@Composable
fun NavGraph(
    onPlayMedia: (mediaId: Long, path: String) -> Unit,
    onPlayMediaFromFolder: (mediaId: Long, path: String, folderPath: String) -> Unit = { id, path, _ -> onPlayMedia(id, path) }
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onMediaClick = { media ->
                    onPlayMedia(media.id, media.path)
                },
                onFolderClick = { folderPath ->
                    val folderName = folderPath.substringAfterLast("/").ifEmpty { "Folder" }
                    navController.navigate(Routes.folderRoute(folderPath, folderName))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.FOLDER,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderPath = URLDecoder.decode(backStackEntry.arguments?.getString("folderPath") ?: "", "UTF-8")
            val folderName = URLDecoder.decode(backStackEntry.arguments?.getString("folderName") ?: "", "UTF-8")
            FolderScreen(
                folderPath = folderPath,
                folderName = folderName,
                onMediaClick = { media ->
                    onPlayMediaFromFolder(media.id, media.path, folderPath)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
