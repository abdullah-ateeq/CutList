package com.example.cutlist.ui.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cutlist.ui.CutListViewModel
import com.example.cutlist.ui.PendingConfirmation
import com.example.cutlist.ui.UiEvent
import com.example.cutlist.ui.about.AboutScreen
import com.example.cutlist.ui.components.ConfirmDialog
import com.example.cutlist.ui.cutlist.CutListScreen
import com.example.cutlist.ui.history.HistoryScreen
import com.example.cutlist.ui.settings.SettingsScreen
import com.example.cutlist.ui.splash.SplashScreen
import com.example.cutlist.ui.theme.CutListTheme
import kotlinx.coroutines.launch

object Routes {
    const val CUT_LIST = "cutList"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val DESTINATIONS = listOf(
    TopLevelDestination(Routes.CUT_LIST, "Cut List", Icons.Filled.Straighten, Icons.Outlined.Straighten),
    TopLevelDestination(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    TopLevelDestination(Routes.ABOUT, "About", Icons.Filled.Info, Icons.Outlined.Info),
)

@Composable
fun CutListApp(viewModel: CutListViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    CutListTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
        Crossfade(targetState = showSplash, label = "splash") { splash ->
            if (splash) {
                SplashScreen(isReady = isReady, onFinished = { showSplash = false })
            } else {
                MainScaffold(viewModel)
            }
        }
    }
}

@Composable
private fun MainScaffold(viewModel: CutListViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val hapticsEnabled by rememberUpdatedState(settings.hapticFeedback)

    val performHaptic: () -> Unit = {
        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Message -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = event.text,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) event.onAction?.invoke()
                }
                UiEvent.Haptic -> performHaptic()
                UiEvent.ShowCutList -> navController.navigateTopLevel(Routes.CUT_LIST)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { BottomBar(navController) },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = Routes.CUT_LIST,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable(Routes.CUT_LIST) { CutListScreen(viewModel, snackbarHostState, performHaptic) }
            composable(Routes.HISTORY) { HistoryScreen(viewModel, snackbarHostState) }
            composable(Routes.SETTINGS) { SettingsScreen(viewModel, snackbarHostState) }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }

    pending?.let { confirmation ->
        PendingDialog(confirmation, onConfirm = viewModel::confirmPending, onDismiss = viewModel::dismissPending)
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        DESTINATIONS.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) navController.navigateTopLevel(destination.route) },
                icon = { Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PendingDialog(confirmation: PendingConfirmation, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (confirmation) {
        PendingConfirmation.NewCutList -> ConfirmDialog(
            title = "Start a new cut list?",
            message = "Unsaved changes to the current cut list will be lost. Saved history is not affected.",
            confirmLabel = "Start new",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is PendingConfirmation.DeleteRow -> ConfirmDialog(
            title = "Delete piece?",
            message = if (confirmation.row.isMerged) {
                "“${confirmation.row.piece.name}” (${confirmation.row.sourceIds.size} combined entries) will be removed from this cut list."
            } else {
                "“${confirmation.row.piece.name}” will be removed from this cut list."
            },
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is PendingConfirmation.OpenProject -> ConfirmDialog(
            title = "Open saved cut list?",
            message = "The current cut list has unsaved changes. Opening “${confirmation.project.displayName}” will replace it.",
            confirmLabel = "Open",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is PendingConfirmation.DeleteHistory -> ConfirmDialog(
            title = "Delete saved cut list?",
            message = "“${confirmation.project.displayName}” will be removed from history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        PendingConfirmation.ClearHistory -> ConfirmDialog(
            title = "Clear all history?",
            message = "All saved cut lists will be permanently removed. The cut list you are editing stays open.",
            confirmLabel = "Clear all",
            destructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        PendingConfirmation.ResetSettings -> ConfirmDialog(
            title = "Reset settings?",
            message = "All settings return to their defaults. Cut lists and history are not affected.",
            confirmLabel = "Reset",
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
