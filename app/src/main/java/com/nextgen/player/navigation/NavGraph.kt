package com.nextgen.player.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nextgen.player.library.ui.FolderScreen
import com.nextgen.player.library.ui.LibraryScreen
import com.nextgen.player.network.ui.NetworkScreen
import com.nextgen.player.network.ui.ServerBrowserScreen
import com.nextgen.player.ui.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LIBRARY = "library"
    const val FOLDER = "folder/{folderPath}/{folderName}"
    const val SETTINGS = "settings"
    const val NETWORK = "network"
    const val NETWORK_BROWSE_SERVER = "network/browse/{serverId}"
    const val NETWORK_BROWSE_DLNA = "network/dlna?location={location}&name={name}"

    fun folderRoute(folderPath: String, folderName: String): String {
        val encodedPath = URLEncoder.encode(folderPath, "UTF-8")
        val encodedName = URLEncoder.encode(folderName, "UTF-8")
        return "folder/$encodedPath/$encodedName"
    }

    fun networkBrowseServer(serverId: Long): String = "network/browse/$serverId"

    fun networkBrowseDlna(location: String, name: String): String {
        val encodedLocation = URLEncoder.encode(location, "UTF-8")
        val encodedName = URLEncoder.encode(name, "UTF-8")
        return "network/dlna?location=$encodedLocation&name=$encodedName"
    }
}

@Composable
fun NavGraph(
    onPlayMedia: (mediaId: Long, path: String) -> Unit,
    onPlayMediaFromFolder: (mediaId: Long, path: String, folderPath: String) -> Unit = { id, path, _ -> onPlayMedia(id, path) },
    onPlayUrl: (url: String) -> Unit = {}
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
                },
                onNetworkClick = {
                    navController.navigate(Routes.NETWORK)
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

        composable(Routes.NETWORK) {
            NetworkScreen(
                onBackClick = { navController.popBackStack() },
                onBrowseServer = { serverId ->
                    navController.navigate(Routes.networkBrowseServer(serverId))
                },
                onBrowseDlna = { location, name ->
                    navController.navigate(Routes.networkBrowseDlna(location, name))
                },
                onPlayUrl = { url -> onPlayUrl(url) }
            )
        }

        composable(
            route = Routes.NETWORK_BROWSE_SERVER,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: -1L
            ServerBrowserScreen(
                serverId = serverId,
                onBackClick = { navController.popBackStack() },
                onPlayFile = { url -> onPlayUrl(url) }
            )
        }

        composable(
            route = Routes.NETWORK_BROWSE_DLNA,
            arguments = listOf(
                navArgument("location") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val location = backStackEntry.arguments?.getString("location") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            ServerBrowserScreen(
                dlnaLocation = location,
                dlnaName = name,
                onBackClick = { navController.popBackStack() },
                onPlayFile = { url -> onPlayUrl(url) }
            )
        }
    }
}
