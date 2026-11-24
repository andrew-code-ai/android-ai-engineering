package com.example.claims.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.claims.R
import com.example.claims.domain.Claim
import com.example.claims.ui.common.MoneyFormat
import com.example.claims.ui.common.label

@Composable
fun ClaimListRoute(onAddClaim: () -> Unit, viewModel: ClaimListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ClaimListScreen(state = state, onAddClaim = onAddClaim)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimListScreen(state: ClaimListUiState, onAddClaim: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.claims_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClaim,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.claims_add)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ClaimListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ClaimListUiState.Content ->
                    if (state.claims.isEmpty()) {
                        Text(stringResource(R.string.claims_empty), Modifier.align(Alignment.Center).padding(24.dp))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.claims, key = { it.id.value }) { ClaimRow(it) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun ClaimRow(claim: Claim) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(claim.number, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(claim.status.label()), style = MaterialTheme.typography.labelLarge)
            }
            Text("${stringResource(claim.type.label())} · ${claim.incidentDate}")
            Text(MoneyFormat.format(claim.estimatedLoss), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
