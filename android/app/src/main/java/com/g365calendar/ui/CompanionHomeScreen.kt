package com.g365calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.g365calendar.auth.AuthState
import com.g365calendar.data.model.Calendar

@Composable
fun companionHomeScreen(
    viewModel: MainViewModel,
    onNavigateToCalendarSelection: () -> Unit,
) {
    val authStatus by viewModel.authStatus.collectAsState()
    val selectedCalendars by viewModel.selectedCalendars.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "G365 Calendar",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (authStatus) {
            is AuthState.Status.Loading, is AuthState.Status.Unknown -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Status.Unauthenticated -> {
                signInSection(viewModel)
            }
            is AuthState.Status.Authenticated -> {
                authenticatedSection(
                    displayName = (authStatus as AuthState.Status.Authenticated).displayName,
                    selectedCalendars = selectedCalendars,
                    syncStatus = syncStatus,
                    onSelectCalendars = onNavigateToCalendarSelection,
                    onSignOut = { viewModel.signOut() },
                )
            }
            is AuthState.Status.Error -> {
                Text(
                    text = (authStatus as AuthState.Status.Error).message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                signInSection(viewModel)
            }
        }
    }
}

@Composable
private fun signInSection(viewModel: MainViewModel) {
    val activity = LocalContext.current as? android.app.Activity

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Sign in with your Microsoft 365 account to sync calendar events to your Garmin watch.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { activity?.let { viewModel.signIn(it) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in with Microsoft")
        }
    }
}

@Composable
private fun authenticatedSection(
    displayName: String?,
    selectedCalendars: List<Calendar>,
    syncStatus: SyncUiState,
    onSelectCalendars: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium)
                Text(displayName ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selected calendars", style = MaterialTheme.typography.labelMedium)
                if (selectedCalendars.isEmpty()) {
                    Text("No calendars selected", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedCalendars.forEach { calendar ->
                            selectedCalendarRow(calendar)
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onSelectCalendars, modifier = Modifier.fillMaxWidth()) {
            Text("Select Calendars")
        }

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun selectedCalendarRow(calendar: Calendar) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val colorValue = calendar.hexColor?.let(::parseHexColor) ?: calendar.color?.let(::parseHexColor)
        if (colorValue != null) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colorValue),
            )
        }

        Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun parseHexColor(hex: String): Color? {
    return try {
        val cleaned = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$cleaned"))
    } catch (_: Exception) {
        null
    }
}
