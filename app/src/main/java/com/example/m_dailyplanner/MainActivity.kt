package com.example.m_dailyplanner

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.m_dailyplanner.data.DataStoreManager
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.data.TaskRepository
import com.example.m_dailyplanner.ui.DayDetailScreen
import com.example.m_dailyplanner.ui.HomeScreen
import com.example.m_dailyplanner.ui.OnboardingScreen
import com.example.m_dailyplanner.ui.theme.MDailyPlannerTheme
import com.example.m_dailyplanner.viewmodel.TaskViewModel
import com.example.m_dailyplanner.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MDailyPlannerTheme {
                val database = remember { TaskDatabase.getDatabase(applicationContext) }
                val repository = remember { TaskRepository(database.taskDao()) }
                val dataStoreManager = remember { DataStoreManager(applicationContext) }
                
                val viewModel: TaskViewModel = viewModel(
                    factory = TaskViewModelFactory(application, repository, dataStoreManager)
                )

                val showOnboarding by viewModel.showOnboarding.collectAsState()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(Unit) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val navController = rememberNavController()

                if (showOnboarding) {
                    OnboardingScreen(onFinished = { viewModel.completeOnboarding() })
                } else {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onDayClick = { date ->
                                    navController.navigate("detail/$date")
                                }
                            )
                        }
                        composable(
                            "detail/{date}",
                            arguments = listOf(navArgument("date") { })
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            DayDetailScreen(
                                date = date,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
