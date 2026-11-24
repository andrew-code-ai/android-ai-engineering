package com.example.claims.ui.newclaim

import app.cash.turbine.test
import com.example.claims.R
import com.example.claims.domain.Claim
import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.ClaimId
import com.example.claims.domain.ClaimRepository
import com.example.claims.domain.ClaimStatus
import com.example.claims.domain.ClaimType
import com.example.claims.domain.PolicyNumber
import com.example.claims.domain.PreferencesRepository
import com.example.claims.domain.SubmitClaimUseCase
import com.example.claims.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class NewClaimViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 24)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private class FakeRepo(private val active: Boolean = true) : ClaimRepository {
        var submittedDraft: ClaimDraft? = null
        override fun observeClaims(): Flow<List<Claim>> = emptyFlow()
        override suspend fun isPolicyActive(policy: PolicyNumber) = active
        override suspend fun submit(draft: ClaimDraft): Claim {
            submittedDraft = draft
            return Claim(ClaimId("1"), "SZK-2026-000001", draft.policyNumber, draft.type, draft.incidentDate,
                         draft.estimatedLoss, draft.description, ClaimStatus.SUBMITTED, Instant.EPOCH)
        }
    }

    private class FakePrefs : PreferencesRepository {
        override val lastClaimType = MutableStateFlow(ClaimType.HOME)
        override suspend fun setLastClaimType(type: ClaimType) { lastClaimType.value = type }
    }

    private fun viewModel(repo: ClaimRepository = FakeRepo()) =
        NewClaimViewModel(SubmitClaimUseCase(repo, FakePrefs(), clock), FakePrefs())

    private fun NewClaimViewModel.fillValid() {
        onPolicyChange("pol-2024-00012345")
        onDateChange("2026-11-20")
        onLossChange("1 250,50")
        onDescriptionChange("Zalanie łazienki przez sąsiada z góry")
    }

    @Test
    fun `restores last used claim type`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ClaimType.HOME, vm.uiState.value.type)
    }

    @Test
    fun `invalid formats are reported per field without calling repository`() = runTest {
        val repo = FakeRepo()
        val vm = viewModel(repo)
        vm.onPolicyChange("12345")
        vm.onDateChange("24.11.2026")
        vm.onLossChange("dużo")
        vm.onSubmit()
        advanceUntilIdle()

        val errors = vm.uiState.value.errors
        assertEquals(R.string.error_policy_format, errors[Field.POLICY])
        assertEquals(R.string.error_date_format, errors[Field.DATE])
        assertEquals(R.string.error_loss_format, errors[Field.LOSS])
        assertEquals(null, repo.submittedDraft)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val vm = viewModel()
        vm.onSubmit()
        vm.onPolicyChange("POL-2024-00012345")
        assertFalse(Field.POLICY in vm.uiState.value.errors)
    }

    @Test
    fun `domain validation errors are mapped to fields`() = runTest {
        val vm = viewModel()
        vm.fillValid()
        vm.onDescriptionChange("za krótko")
        vm.onSubmit()
        advanceUntilIdle()
        assertEquals(R.string.error_description, vm.uiState.value.errors[Field.DESCRIPTION])
    }

    @Test
    fun `inactive policy shows policy error`() = runTest {
        val vm = viewModel(FakeRepo(active = false))
        vm.fillValid()
        vm.onSubmit()
        advanceUntilIdle()
        assertEquals(R.string.error_policy_inactive, vm.uiState.value.errors[Field.POLICY])
    }

    @Test
    fun `successful submit emits one-off event with claim number`() = runTest {
        val repo = FakeRepo()
        val vm = viewModel(repo)
        vm.fillValid()
        vm.events.test {
            vm.onSubmit()
            assertTrue(vm.uiState.value.isSubmitting)
            assertEquals(NewClaimEvent.Submitted("SZK-2026-000001"), awaitItem())
        }
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals(125_050L, repo.submittedDraft?.estimatedLoss?.grosze)
    }
}
