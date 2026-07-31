package com.cnctech.process

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cnctech.process.ui.components.AppHeader
import com.cnctech.process.ui.components.AutoDismissSnackbarHost
import com.cnctech.process.ui.components.showAppError
import com.cnctech.process.ui.navigation.Screen
import com.cnctech.process.ui.navigation.title
import com.cnctech.process.ui.screens.assembly.AssemblyScreen
import com.cnctech.process.ui.screens.backup.BackupScreen
import com.cnctech.process.ui.screens.catalog.CatalogGalleryScreen
import com.cnctech.process.ui.screens.catalog.CatalogScreen
import com.cnctech.process.ui.screens.hub.HubScreen
import com.cnctech.process.ui.screens.parts.PartDetailScreen
import com.cnctech.process.ui.screens.parts.PartEditScreen
import com.cnctech.process.ui.screens.parts.PartGalleryScreen
import com.cnctech.process.ui.screens.parts.PartsListScreen
import com.cnctech.process.ui.screens.techprocess.SetupScreen
import com.cnctech.process.ui.screens.techprocess.TechProcessScreen
import com.cnctech.process.ui.theme.CncBackground
import com.cnctech.process.ui.theme.CncTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CncTheme {
                CncAppRoot()
            }
        }
    }
}

@Composable
private fun CncAppRoot() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Hub) }
    val route = stack.last()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun showError(msg: String) {
        scope.launch { snackbar.showAppError(msg) }
    }

    fun showInfo(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CncBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = route.title(),
                showBack = stack.size > 1,
                onBack = { pop() },
            )
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when (val r = route) {
                    Screen.Hub -> HubScreen(
                        onOpenParts = { push(Screen.Parts) },
                        onOpenCatalog = { push(Screen.Catalog) },
                        onOpenBackup = { push(Screen.Backup) },
                    )
                    Screen.Parts -> PartsListScreen(
                        onOpenPart = { push(Screen.Part(it)) },
                        onError = ::showError,
                    )
                    is Screen.Part -> PartDetailScreen(
                        partId = r.partId,
                        onEdit = { push(Screen.PartEdit(r.partId)) },
                        onOpenGallery = { push(Screen.PartGallery(r.partId)) },
                        onOpenTechProcess = { push(Screen.TechProcess(r.partId)) },
                        onOpenAssembly = { push(Screen.Assembly(r.partId)) },
                    )
                    is Screen.PartEdit -> PartEditScreen(
                        partId = r.partId,
                        onSaved = { pop() },
                        onDeleted = {
                            // pop edit + part
                            if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                            if (stack.isNotEmpty() && stack.last() is Screen.Part) {
                                stack.removeAt(stack.lastIndex)
                            }
                        },
                        onError = ::showError,
                    )
                    is Screen.PartGallery -> PartGalleryScreen(
                        partId = r.partId,
                        onError = ::showError,
                    )
                    is Screen.TechProcess -> TechProcessScreen(
                        partId = r.partId,
                        onOpenSetup = { setupId -> push(Screen.Setup(r.partId, setupId)) },
                        onError = ::showError,
                    )
                    is Screen.Setup -> SetupScreen(
                        setupId = r.setupId,
                        onDeleted = { pop() },
                        onError = ::showError,
                    )
                    is Screen.Assembly -> AssemblyScreen(
                        partId = r.partId,
                        onError = ::showError,
                    )
                    Screen.Catalog -> CatalogScreen(
                        onOpenGallery = { id, title -> push(Screen.CatalogGallery(id, title)) },
                        onError = ::showError,
                    )
                    is Screen.CatalogGallery -> CatalogGalleryScreen(
                        catalogItemId = r.catalogItemId,
                        title = r.title,
                        onError = ::showError,
                    )
                    Screen.Backup -> BackupScreen(
                        onError = ::showError,
                        onInfo = ::showInfo,
                    )
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            AutoDismissSnackbarHost(hostState = snackbar)
        }
    }
}
