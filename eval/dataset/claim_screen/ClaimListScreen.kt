package com.example.claims.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ClaimListScreen(navController: NavController, filter: String) {
    val viewModel: ClaimListViewModel = viewModel()
    val claims by viewModel.claims.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(filter)
    }

    val sorted = claims.sortedByDescending { it.createdAt }.filter { it.status.contains(filter) }

    Column {
        Text("Twoje szkody: ${sorted.size}")
        LazyColumn {
            items(sorted) { claim ->
                ClaimRow(claim, viewModel)
            }
        }
        IconButton(onClick = { viewModel.submitNew(navController) }) {
            Text("+")
        }
    }
}

@Composable
fun ClaimRow(claim: ClaimUi, viewModel: ClaimListViewModel) {
    Column {
        Text("${claim.number} — ${claim.amount} zł")
        IconButton(onClick = { viewModel.delete(claim.id) }) {
            Icon(Icons.Default.Delete, contentDescription = null)
        }
    }
}
