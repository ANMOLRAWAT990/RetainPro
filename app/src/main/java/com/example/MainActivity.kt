package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.DesignScreen
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.SavedLogsScreen
import com.example.ui.screens.WalkthroughScreen
import com.example.ui.screens.EstimateScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WallViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: WallViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "design"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Design") },
                    label = { Text("Design", maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    selected = currentRoute == "design",
                    onClick = {
                        navController.navigate("design") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                    label = { Text("Analysis", maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    selected = currentRoute == "analysis",
                    onClick = {
                        navController.navigate("analysis") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Architecture, contentDescription = "Drawing") },
                    label = { Text("Drawing", maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    selected = currentRoute == "drawing",
                    onClick = {
                        navController.navigate("drawing") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Estimate") },
                    label = { Text("Estimate", maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    selected = currentRoute == "estimate",
                    onClick = {
                        navController.navigate("estimate") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                    label = { Text("Logs", maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    selected = currentRoute == "logs",
                    onClick = {
                        navController.navigate("logs") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val params by viewModel.wallParams.collectAsState()
        val analysisResult by viewModel.analysisResult.collectAsState()
        val savedLogs by viewModel.savedLogs.collectAsState(initial = emptyList())

        NavHost(
            navController = navController,
            startDestination = "design",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("design") {
                DesignScreen(
                    params = params,
                    onParamChange = { viewModel.updateParam { _ -> it } },
                    onAnalyze = {
                        viewModel.analyze()
                        navController.navigate("analysis") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReset = { viewModel.reset() },
                    onSave = { 
                        viewModel.saveCurrentDesign() 
                        navController.navigate("logs") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("analysis") {
                AnalysisScreen(result = analysisResult)
            }
            composable("drawing") {
                DrawingScreen(params = params)
            }
            composable("estimate") {
                EstimateScreen(
                    savedLogs = savedLogs,
                    currentParams = params,
                    onSaveRates = { newRates -> viewModel.updateParam { it.copy(rates = newRates) } }
                )
            }
            composable("logs") {
                SavedLogsScreen(
                    savedLogs = savedLogs,
                    onLoad = { log ->
                        viewModel.loadDesign(log)
                        navController.navigate("design") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDelete = { id -> viewModel.deleteDesign(id) }
                )
            }
            composable("walkthrough") {
                WalkthroughScreen()
            }
        }
    }
}
