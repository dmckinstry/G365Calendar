package com.g365calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.g365calendar.auth.AuthState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CompanionHomeScreen(
    viewModel: MainViewModel,
    onNavigateToCalendarSelection: () -> Unit,
) {
    val authStatus by viewModel.authStatus.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                SignInSection(viewModel)
            }
            is AuthState.Status.Authenticated -> {
                AuthenticatedSection(
                    displayName = (authStatus as AuthState.Status.Authenticated).displayName,
                    syncStatus = syncStatus,
                    lastSyncTime = lastSyncTime,
                    onSync = { viewModel.triggerSync() },
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
                SignInSection(viewModel)
            }
        }
    }
}

@Composable
private fun SignInSection(viewModel: MainViewModel) {
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
private fun AuthenticatedSection(
    displayName: String?,
    syncStatus: SyncUiState,
    lastSyncTime: Long?,
    onSync: () -> Unit,
    onSelectCalendars: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium)
                Text(displayName ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sync Status", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = when (syncStatus) {
                        is SyncUiState.Idle -> "Not yet synced"
                        is SyncUiState.Syncing -> "Syncing…"
                        is SyncUiState.Success -> "${syncStatus.eventCount} events synced"
                        is SyncUiState.Error -> "Error: ${syncStatus.message}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (lastSyncTime != null) {
                    val formatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(lastSyncTime))
                    Text("Last sync: $formatted", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Button(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
            if (syncStatus is SyncUiState.Syncing) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Sync Now")
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
