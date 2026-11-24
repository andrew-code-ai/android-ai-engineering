package com.example.claims.domain

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubmitClaimUseCaseTest {

    private val today = LocalDate(2026, 11, 24)
    private val clock = FixedClock(today.atStartOfDayIn(TimeZone.UTC))
    private val repo = FakeClaimRepository()
    private val prefs = FakePreferencesRepository()
    private val submit = SubmitClaimUseCase(repo, prefs, clock, TimeZone.UTC)

    private fun draft(
        policy: String = FakeClaimRepository.ACTIVE_POLICY,
        date: LocalDate = today.minus(2, DateTimeUnit.DAY),
        loss: Money = Money.ofZloty(1_200),
        description: String = "Stłuczka na parkingu, uszkodzony zderzak",
    ) = ClaimDraft(PolicyNumber(policy), ClaimType.HOME, date, loss, description)

    @Test
    fun validDraftOnActivePolicyIsSubmittedAndRemembersClaimType() = runTest {
        assertIs<SubmitClaimResult.Submitted>(submit(draft()))
        assertEquals(ClaimType.HOME, prefs.lastClaimType.value)
    }

    @Test
    fun inactivePolicyIsReportedWithoutSubmitting() = runTest {
        assertEquals(SubmitClaimResult.PolicyInactive, submit(draft(policy = "POL-2024-99999999")))
        assertTrue(repo.submitted.isEmpty())
    }

    @Test
    fun allValidationErrorsAreReturnedTogether() = runTest {
        val result = submit(draft(date = today.plus(1, DateTimeUnit.DAY), loss = Money.ZERO, description = "krótko"))
        assertEquals(
            SubmitClaimResult.Invalid(
                setOf(ClaimValidationError.INCIDENT_IN_FUTURE, ClaimValidationError.LOSS_ZERO,
                      ClaimValidationError.DESCRIPTION_TOO_SHORT)
            ),
            result,
        )
    }

    @Test
    fun incidentOlderThanReportingWindowIsRejected() = runTest {
        val result = submit(draft(date = today.minus(ClaimRules.MAX_REPORTING_DAYS + 1, DateTimeUnit.DAY)))
        assertEquals(SubmitClaimResult.Invalid(setOf(ClaimValidationError.INCIDENT_TOO_OLD)), result)
    }

    @Test
    fun repositoryFailureIsWrappedNotThrown() = runTest {
        repo.failWith = IllegalStateException("offline")
        assertIs<SubmitClaimResult.Failed>(submit(draft()))
    }
}
