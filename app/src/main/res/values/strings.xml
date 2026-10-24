package com.sesmartcell.notaservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.sesmartcell.notaservice.auth.AppUser
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.screens.AuthScreen
import com.sesmartcell.notaservice.ui.screens.CreateEditNoteScreen
import com.sesmartcell.notaservice.ui.screens.DashboardScreen
import com.sesmartcell.notaservice.ui.screens.DocumentationScreen
import com.sesmartcell.notaservice.ui.screens.ReportsScreen
import com.sesmartcell.notaservice.ui.screens.ServiceDetailScreen
import com.sesmartcell.notaservice.ui.screens.ServiceListScreen
import com.sesmartcell.notaservice.ui.screens.SettingsScreen
import com.sesmartcell.notaservice.ui.theme.MyApplicationTheme
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue

enum class AppScreen {
    DASHBOARD,
    CREATE_EDIT_NOTE,
    SERVICE_LIST,
    NOTE_DETAIL,
    REPORTS,
    SETTINGS,
    DOCUMENTATION
}

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: ServiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: ServiceViewModel) {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(AuthManager.getAppUser(context)) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { _ ->
            currentUser = AuthManager.getAppUser(context)
        }
        AuthManager.auth.addAuthStateListener(listener)
        onDispose {
            AuthManager.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                currentUser = AuthManager.getAppUser(context)
            }
        )
    } else {
        MainAppContainer(
            viewModel = viewModel,
            currentUser = currentUser!!
        )
    }
}

@Composable
fun MainAppContainer(
    viewModel: ServiceViewModel,
    currentUser: AppUser
) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var selectedNoteId by remember { mutableLongStateOf(0L) }
    var editNoteId by remember { mutableStateOf<Long?>(null) }

    val navItems = listOf(
        NavItem(AppScreen.DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
        NavItem(AppScreen.CREATE_EDIT_NOTE, "Buat Nota", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_create_note"),
        NavItem(AppScreen.SERVICE_LIST, "Data Service", Icons.Filled.ListAlt, Icons.Outlined.ListAlt, "nav_service_list"),
        NavItem(AppScreen.REPORTS, "Laporan", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports"),
        NavItem(AppScreen.SETTINGS, "Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val showBottomBar = currentScreen in listOf(
        AppScreen.DASHBOARD,
        AppScreen.SERVICE_LIST,
        AppScreen.REPORTS,
        AppScreen.SETTINGS
    )

    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        when (currentScreen) {
            AppScreen.NOTE_DETAIL -> currentScreen = AppScreen.DASHBOARD
            AppScreen.CREATE_EDIT_NOTE -> currentScreen = AppScreen.DASHBOARD
            AppScreen.DOCUMENTATION -> currentScreen = AppScreen.SETTINGS
            else -> currentScreen = AppScreen.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (item.screen == AppScreen.CREATE_EDIT_NOTE) {
                                    editNoteId = null
                                }
                                currentScreen = item.screen
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = PrimaryBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCreate = {
                            editNoteId = null
                            currentScreen = AppScreen.CREATE_EDIT_NOTE
                        },
                        onNavigateToDetail = { id ->
                            selectedNoteId = id
                            currentScreen = AppScreen.NOTE_DETAIL
                        },
                        onNavigateToAllNotes = {
                            currentScreen = AppScreen.SERVICE_LIST
                        }
                    )
                }

                AppScreen.CREATE_EDIT_NOTE -> {
                    CreateEditNoteScreen(
                        viewModel = viewModel,
                        existingNoteId = editNoteId,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                        onNoteSaved = { savedId ->
                            selectedNoteId = savedId
                            currentScreen = AppScreen.NOTE_DETAIL
                        }
                    )
                }

                AppScreen.SERVICE_LIST -> {
                    ServiceListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                        onNavigateToCreate = {
                            editNoteId = null
                            currentScreen = AppScreen.CREATE_EDIT_NOTE
                        },
                        onNavigateToDetail = { id ->
                            selectedNoteId = id
                            currentScreen = AppScreen.NOTE_DETAIL
                        },
                        onNavigateToEdit = { id ->
                            editNoteId = id
                            currentScreen = AppScreen.CREATE_EDIT_NOTE
                        }
                    )
                }

                AppScreen.NOTE_DETAIL -> {
                    ServiceDetailScreen(
                        noteId = selectedNoteId,
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                        onNavigateToEdit = { id ->
                            editNoteId = id
                            currentScreen = AppScreen.CREATE_EDIT_NOTE
                        }
                    )
                }

                AppScreen.REPORTS -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD },
                        onNavigateToDocs = { currentScreen = AppScreen.DOCUMENTATION }
                    )
                }

                AppScreen.DOCUMENTATION -> {
                    DocumentationScreen(
                        onNavigateBack = { currentScreen = AppScreen.SETTINGS }
                    )
                }
            }
        }
    }
}

