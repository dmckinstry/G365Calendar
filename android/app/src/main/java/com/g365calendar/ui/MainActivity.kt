package com.g365calendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.g365calendar.sync.GarminConnector
import com.g365calendar.ui.theme.g365CalendarTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var garminConnector: GarminConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        garminConnector.initialize(this, autoUi = true)
        enableEdgeToEdge()
        setContent {
            g365CalendarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val navController = rememberNavController()
                        val mainViewModel: MainViewModel = hiltViewModel()

                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                companionHomeScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToCalendarSelection = {
                                        navController.navigate("calendar_selection")
                                    },
                                )
                            }
                            composable("calendar_selection") {
                                calendarSelectionScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
