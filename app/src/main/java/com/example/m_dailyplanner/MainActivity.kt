package com.example.m_dailyplanner

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.m_dailyplanner.auth.AuthManager
import com.example.m_dailyplanner.auth.AuthViewModel
import com.example.m_dailyplanner.auth.AuthViewModelFactory
import com.example.m_dailyplanner.data.*
import com.example.m_dailyplanner.sync.FirestoreSync
import com.example.m_dailyplanner.ui.*
import com.example.m_dailyplanner.ui.theme.MDailyPlannerTheme
import com.example.m_dailyplanner.viewmodel.*

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("tasks", "Tasks", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    BottomNavItem("projects", "Projects", Icons.Filled.Folder, Icons.Outlined.FolderOpen),
    BottomNavItem("notes", "Notes", Icons.Filled.Description, Icons.Outlined.Description)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MDailyPlannerTheme {
                val database = remember { TaskDatabase.getDatabase(applicationContext) }
                val firestoreSync = remember { FirestoreSync() }
                val authManager = remember { AuthManager() }

                val taskRepository = remember { TaskRepository(database.taskDao(), firestoreSync) }
                val projectRepository = remember {
                    ProjectRepository(database.projectDao(), database.projectTaskDao(), firestoreSync)
                }
                val noteRepository = remember { NoteRepository(database.noteDao(), database.noteCategoryDao(), firestoreSync) }
                val dataStoreManager = remember { DataStoreManager(applicationContext) }

                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(application, authManager, firestoreSync, database)
                )
                val taskViewModel: TaskViewModel = viewModel(
                    factory = TaskViewModelFactory(application, taskRepository, dataStoreManager)
                )
                val projectViewModel: ProjectViewModel = viewModel(
                    factory = ProjectViewModelFactory(application, projectRepository)
                )
                val noteViewModel: NoteViewModel = viewModel(
                    factory = NoteViewModelFactory(application, noteRepository)
                )

                val currentUser by authViewModel.currentUser.collectAsState()
                val showOnboarding by taskViewModel.showOnboarding.collectAsState()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }

                when {
                    showOnboarding -> {
                        OnboardingScreen(onFinished = { taskViewModel.completeOnboarding() })
                    }
                    else -> {
                        MainApp(
                            taskViewModel = taskViewModel,
                            projectViewModel = projectViewModel,
                            noteViewModel = noteViewModel,
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainApp(
    taskViewModel: TaskViewModel,
    projectViewModel: ProjectViewModel,
    noteViewModel: NoteViewModel,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val topLevelRoutes = setOf("tasks", "projects", "notes")

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "tasks",
            modifier = Modifier.padding(padding)
        ) {
            // ── Tasks tab ──
            composable("tasks") {
                HomeScreen(
                    viewModel = taskViewModel,
                    onDayClick = { date -> navController.navigate("detail/$date") },
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                    onNavigateToPending = { navController.navigate("pending_tasks") }
                )
            }
            composable("pending_tasks") {
                AllPendingTasksScreen(
                    viewModel = taskViewModel,
                    onBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
                )
            }
            composable(
                "detail/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                DayDetailScreen(
                    date = date,
                    viewModel = taskViewModel,
                    onBack = { navController.popBackStack() },
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
                )
            }
            composable(
                "task_detail/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId") ?: -1
                TaskDetailScreen(
                    taskId = taskId,
                    viewModel = taskViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Projects tab ──
            composable("projects") {
                ProjectsScreen(
                    viewModel = projectViewModel,
                    onProjectClick = { projectId ->
                        navController.navigate("project_detail/$projectId")
                    }
                )
            }
            composable(
                "project_detail/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.IntType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getInt("projectId") ?: -1
                val projectName = projectViewModel.projects.value
                    .find { it.id == projectId }?.name ?: "Project"
                ProjectDetailScreen(
                    projectId = projectId,
                    projectName = projectName,
                    viewModel = projectViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Notes tab ──
            composable("notes") {
                NotesScreen(
                    viewModel = noteViewModel,
                    onNoteClick = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNewNote = { categoryId -> navController.navigate("note_detail/0?categoryId=$categoryId") }
                )
            }
            composable(
                "note_detail/{noteId}?categoryId={categoryId}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.IntType },
                    navArgument("categoryId") { type = NavType.IntType; defaultValue = DEFAULT_CATEGORY_ID }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
                val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: DEFAULT_CATEGORY_ID
                NoteDetailScreen(
                    noteId = noteId,
                    defaultCategoryId = categoryId,
                    viewModel = noteViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
