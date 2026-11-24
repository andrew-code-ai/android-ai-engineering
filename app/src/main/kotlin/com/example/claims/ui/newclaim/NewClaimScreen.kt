package com.example.claims.ui.newclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.claims.R
import com.example.claims.domain.ClaimType
import com.example.claims.ui.common.label

@Composable
fun NewClaimRoute(
    onBack: () -> Unit,
    onSubmitted: (claimNumber: String) -> Unit,
    viewModel: NewClaimViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSubmitted by rememberUpdatedState(onSubmitted)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NewClaimEvent.Submitted -> currentOnSubmitted(event.claimNumber)
            }
        }
    }
    NewClaimScreen(
        state = state,
        actions = NewClaimActions(
            onPolicyChange = viewModel::onPolicyChange,
            onTypeChange = viewModel::onTypeChange,
            onDateChange = viewModel::onDateChange,
            onLossChange = viewModel::onLossChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSubmit = viewModel::onSubmit,
            onBack = onBack,
        ),
    )
}

data class NewClaimActions(
    val onPolicyChange: (String) -> Unit = {},
    val onTypeChange: (ClaimType) -> Unit = {},
    val onDateChange: (String) -> Unit = {},
    val onLossChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

object NewClaimTags {
    const val POLICY = "field_policy"
    const val DATE = "field_date"
    const val LOSS = "field_loss"
    const val DESCRIPTION = "field_description"
    const val SUBMIT = "submit"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClaimScreen(state: NewClaimUiState, actions: NewClaimActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_claim_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(state.policyNumber, actions.onPolicyChange, R.string.field_policy,
                      state.errors[Field.POLICY], NewClaimTags.POLICY)

            Text(stringResource(R.string.field_type), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClaimType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { actions.onTypeChange(type) },
                        label = { Text(stringResource(type.label())) },
                    )
                }
            }

            FormField(state.incidentDate, actions.onDateChange, R.string.field_date,
                      state.errors[Field.DATE], NewClaimTags.DATE, KeyboardType.Number)
            FormField(state.loss, actions.onLossChange, R.string.field_loss,
                      state.errors[Field.LOSS], NewClaimTags.LOSS, KeyboardType.Decimal)
            FormField(state.description, actions.onDescriptionChange, R.string.field_description,
                      state.errors[Field.DESCRIPTION], NewClaimTags.DESCRIPTION, singleLine = false)

            state.formError?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = actions.onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag(NewClaimTags.SUBMIT),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.submit))
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    error: Int?,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
