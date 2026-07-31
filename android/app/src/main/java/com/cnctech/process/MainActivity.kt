package com.cnctech.process

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.ui.components.AppHeader
import com.cnctech.process.ui.components.AutoDismissSnackbarHost
import com.cnctech.process.ui.components.showAppError
import com.cnctech.process.ui.navigation.RootSection
import com.cnctech.process.ui.navigation.Screen
import com.cnctech.process.ui.navigation.isRoot
import com.cnctech.process.ui.navigation.rootSection
import com.cnctech.process.ui.navigation.title
import com.cnctech.process.ui.navigation.toScreen
import com.cnctech.process.ui.screens.assembly.AssemblyScreen
import com.cnctech.process.ui.screens.backup.BackupScreen
import com.cnctech.process.ui.screens.catalog.CatalogGalleryScreen
import com.cnctech.process.ui.screens.catalog.CatalogScreen
import com.cnctech.process.ui.screens.parts.PartDetailScreen
import com.cnctech.process.ui.screens.parts.PartEditScreen
import com.cnctech.process.ui.screens.parts.PartGalleryScreen
import com.cnctech.process.ui.screens.parts.PartsListScreen
import com.cnctech.process.ui.screens.techprocess.SetupScreen
import com.cnctech.process.ui.screens.techprocess.TechProcessScreen
import com.cnctech.process.ui.theme.CncBackground
import com.cnctech.process.ui.theme.CncIconTintBg
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface
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
    val stack = remember { mutableStateListOf<Screen>(Screen.Parts) }
    val route = stack.last()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isRoot = route.isRoot()
    val currentRoot = route.rootSection()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun switchRoot(section: RootSection) {
        stack.clear()
        stack.add(section.toScreen())
        scope.launch { drawerState.close() }
    }

    fun showError(msg: String) {
        scope.launch { snackbar.showAppError(msg) }
    }

    fun showInfo(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isRoot,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CncSurface,
                drawerContentColor = CncOnSurface,
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Техпроцессы ЧПУ",
                        color = CncOnSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    DrawerItem(
                        icon = Icons.Default.PrecisionManufacturing,
                        label = "Детали",
                        selected = currentRoot == RootSection.Parts,
                        onClick = { switchRoot(RootSection.Parts) },
                    )
                    DrawerItem(
                        icon = Icons.Default.Build,
                        label = "Инструмент",
                        selected = currentRoot == RootSection.Catalog,
                        onClick = { switchRoot(RootSection.Catalog) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                    DrawerItem(
                        icon = Icons.Default.Settings,
                        label = "Настройки",
                        selected = currentRoot == RootSection.Settings,
                        onClick = { switchRoot(RootSection.Settings) },
                    )
                }
            }
        },
    ) {
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
                    showBack = !isRoot,
                    onBack = { pop() },
                    showMenu = isRoot,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val r = route) {
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
                        Screen.Settings -> BackupScreen(
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
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) CncIconTintBg else CncSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = CncPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = CncOnSurface,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
