package dev.citali.shadowrpc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.citali.shadowrpc.detection.AppDetectionService
import dev.citali.shadowrpc.ui.screens.AboutScreen
import dev.citali.shadowrpc.ui.screens.AccountScreen
import dev.citali.shadowrpc.ui.screens.DisplayScreen
import dev.citali.shadowrpc.ui.screens.HomeScreen
import dev.citali.shadowrpc.ui.screens.LogsScreen
import dev.citali.shadowrpc.ui.screens.Routes
import dev.citali.shadowrpc.ui.screens.SettingsScreen
import dev.citali.shadowrpc.ui.theme.ShadowRpcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Resume detection if the process was killed while it was on.
        lifecycleScope.launch(Dispatchers.IO) { AppDetectionService.startIfEnabled(this@MainActivity) }
        setContent {
            ShadowRpcTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShadowRpcRoot()
                }
            }
        }
    }
}

private data class DrawerEntry(
    val route: String?,
    val labelRes: Int,
    val icon: ImageVector,
    val url: String? = null,
)

@Composable
private fun ShadowRpcRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val drawerWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).coerceAtMost(304f).dp
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    val mainEntries =
        listOf(
            DrawerEntry(Routes.DISPLAY, R.string.drawer_display, Icons.Outlined.Palette),
            DrawerEntry(Routes.SETTINGS, R.string.drawer_settings, Icons.Outlined.Settings),
        )
    val helpEntries =
        listOf(
            DrawerEntry(null, R.string.drawer_faq, Icons.Outlined.QuestionAnswer, "https://github.com/TherealCitali/ShadowRPC#faq"),
            DrawerEntry(Routes.LOGS, R.string.drawer_logs, Icons.Outlined.Article),
            DrawerEntry(Routes.ABOUT, R.string.drawer_about, Icons.Outlined.Info),
        )

    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    fun open(entry: DrawerEntry) {
        scope.launch { drawerState.close() }
        entry.route?.let(::navigate)
        entry.url?.let { url -> runCatching { uriHandler.openUri(url) } }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth),
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 20.dp),
                    )
                    mainEntries.forEach { entry ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(entry.labelRes)) },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            selected = entry.route != null && backStackEntry?.destination?.route == entry.route,
                            onClick = { open(entry) },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = stringResource(R.string.drawer_help),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    )
                    helpEntries.forEach { entry ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(entry.labelRes)) },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            selected = entry.route != null && backStackEntry?.destination?.route == entry.route,
                            onClick = { open(entry) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigate = ::navigate,
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(Routes.ACCOUNT) { AccountScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.DISPLAY) { DisplayScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.LOGS) { LogsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
