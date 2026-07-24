package com.g365calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.g365calendar.data.model.Calendar

@Composable
fun companionHomeScreen(
    viewModel: MainViewModel,
    onNavigateToCalendarSelection: (String) -> Unit,
) {
    val authStatus by viewModel.authStatus.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

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

        authStatus.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (authStatus.isLoading && accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (accounts.isEmpty()) {
            addAccountEmptyState(viewModel)
            return
        }

        if (authStatus.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            accounts.forEach { accountState ->
                authenticatedSection(
                    accountState = accountState,
                    onSelectCalendars = {
                        viewModel.clearAuthError()
                        onNavigateToCalendarSelection(accountState.account.id)
                    },
                    onSignOut = { viewModel.signOut(accountState.account.id) },
                )
            }

            addAccountButton(viewModel)
        }
    }
}

@Composable
private fun addAccountEmptyState(viewModel: MainViewModel) {
    val activity = LocalContext.current as? android.app.Activity

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Add a Microsoft 365 account to sync calendar events to your Garmin watch.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { activity?.let { viewModel.addAccount(it) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add Microsoft Account")
        }
    }
}

@Composable
private fun addAccountButton(viewModel: MainViewModel) {
    val activity = LocalContext.current as? android.app.Activity

    OutlinedButton(
        onClick = { activity?.let { viewModel.addAccount(it) } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Add Account")
    }
}

@Composable
private fun authenticatedSection(
    accountState: AccountHomeUiState,
    onSelectCalendars: () -> Unit,
    onSignOut: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                accountState.account.username ?: accountState.account.displayName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
            )

            when (val calendarState = accountState.calendarState) {
                CalendarListUiState.Idle -> {
                    Text("Calendar list has not been loaded yet", style = MaterialTheme.typography.bodyMedium)
                }
                CalendarListUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is CalendarListUiState.Error -> {
                    Text(
                        text = calendarState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is CalendarListUiState.Success -> {
                    if (accountState.selectedCalendars.isEmpty()) {
                        Text(
                            text = "${calendarState.calendars.size} calendars available",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (accountState.selectedCalendars.isEmpty()) {
                        Text("No calendars selected", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            accountState.selectedCalendars.forEach { calendar ->
                                selectedCalendarRow(calendar)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = onSelectCalendars,
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("Calendars")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("Sign Out")
                }
            }
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
