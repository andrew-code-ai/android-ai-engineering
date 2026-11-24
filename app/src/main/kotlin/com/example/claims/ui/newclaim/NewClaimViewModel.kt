package com.example.claims.ui.newclaim

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claims.R
import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.ClaimType
import com.example.claims.domain.ClaimValidationError
import com.example.claims.domain.PolicyNumber
import com.example.claims.domain.PreferencesRepository
import com.example.claims.domain.SubmitClaimResult
import com.example.claims.domain.SubmitClaimUseCase
import com.example.claims.ui.common.MoneyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

enum class Field { POLICY, DATE, LOSS, DESCRIPTION }

data class NewClaimUiState(
    val policyNumber: String = "",
    val type: ClaimType = ClaimType.AUTO,
    val incidentDate: String = "",
    val loss: String = "",
    val description: String = "",
    val errors: Map<Field, Int> = emptyMap(),
    @StringRes val formError: Int? = null,
    val isSubmitting: Boolean = false,
)

sealed interface NewClaimEvent {
    data class Submitted(val claimNumber: String) : NewClaimEvent
}

@HiltViewModel
class NewClaimViewModel @Inject constructor(
    private val submitClaim: SubmitClaimUseCase,
    preferences: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewClaimUiState())
    val uiState: StateFlow<NewClaimUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewClaimEvent>(Channel.BUFFERED)
    val events: Flow<NewClaimEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val last = preferences.lastClaimType.first()
            _uiState.update { it.copy(type = last) }
        }
    }

    fun onPolicyChange(value: String) = edit(Field.POLICY) { copy(policyNumber = value) }
    fun onDateChange(value: String) = edit(Field.DATE) { copy(incidentDate = value) }
    fun onLossChange(value: String) = edit(Field.LOSS) { copy(loss = value) }
    fun onDescriptionChange(value: String) = edit(Field.DESCRIPTION) { copy(description = value) }
    fun onTypeChange(type: ClaimType) = _uiState.update { it.copy(type = type) }

    /** Edycja pola czyści jego błąd — użytkownik nie widzi nieaktualnych komunikatów. */
    private inline fun edit(field: Field, crossinline change: NewClaimUiState.() -> NewClaimUiState) =
        _uiState.update { it.change().copy(errors = it.errors - field, formError = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val draft = parseDraft(state) ?: return
        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            when (val result = submitClaim(draft)) {
                is SubmitClaimResult.Submitted -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(NewClaimEvent.Submitted(result.claim.number))
                }
                is SubmitClaimResult.Invalid -> _uiState.update {
                    it.copy(isSubmitting = false, errors = result.errors.associate { e -> e.toFieldError() })
                }
                SubmitClaimResult.PolicyInactive -> _uiState.update {
                    it.copy(isSubmitting = false, errors = mapOf(Field.POLICY to R.string.error_policy_inactive))
                }
                is SubmitClaimResult.Failed -> _uiState.update {
                    it.copy(isSubmitting = false, formError = R.string.error_generic)
                }
            }
        }
    }

    /** Błędy formatu wychwytujemy lokalnie; reguły biznesowe sprawdza domena (SubmitClaimUseCase). */
    private fun parseDraft(state: NewClaimUiState): ClaimDraft? {
        val policy = PolicyNumber.parse(state.policyNumber)
        val date = try {
            LocalDate.parse(state.incidentDate.trim())
        } catch (e: DateTimeParseException) {
            null
        }
        val loss = MoneyFormat.parse(state.loss)

        if (policy == null || date == null || loss == null) {
            val errors = buildMap {
                if (policy == null) put(Field.POLICY, R.string.error_policy_format)
                if (date == null) put(Field.DATE, R.string.error_date_format)
                if (loss == null) put(Field.LOSS, R.string.error_loss_format)
            }
            _uiState.update { it.copy(errors = errors) }
            return null
        }
        return ClaimDraft(policy, state.type, date, loss, state.description.trim())
    }

    private fun ClaimValidationError.toFieldError(): Pair<Field, Int> = when (this) {
        ClaimValidationError.INCIDENT_IN_FUTURE -> Field.DATE to R.string.error_date_future
        ClaimValidationError.INCIDENT_TOO_OLD -> Field.DATE to R.string.error_date_old
        ClaimValidationError.LOSS_ZERO -> Field.LOSS to R.string.error_loss_zero
        ClaimValidationError.DESCRIPTION_TOO_SHORT -> Field.DESCRIPTION to R.string.error_description
    }
}
