package com.example.claims.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class SubmitClaimUseCaseTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val repo = FakeClaimRepository()
    private val prefs = FakePreferencesRepository()
    private val submit = SubmitClaimUseCase(repo, prefs, clock)

    private fun draft(
        policy: String = FakeClaimRepository.ACTIVE_POLICY,
        date: LocalDate = today.minusDays(2),
        loss: Money = Money.ofZloty(1_200),
        description: String = "Stłuczka na parkingu, uszkodzony zderzak",
    ) = ClaimDraft(PolicyNumber(policy), ClaimType.HOME, date, loss, description)

    @Test
    fun `valid draft on active policy is submitted and remembers claim type`() = runTest {
        val result = submit(draft())
        assertTrue(result is SubmitClaimResult.Submitted)
        assertEquals(ClaimType.HOME, prefs.lastClaimType.value)
    }

    @Test
    fun `inactive policy is reported without submitting`() = runTest {
        val result = submit(draft(policy = "POL-2024-99999999"))
        assertEquals(SubmitClaimResult.PolicyInactive, result)
        assertTrue(repo.submitted.isEmpty())
    }

    @Test
    fun `all validation errors are returned together`() = runTest {
        val result = submit(draft(date = today.plusDays(1), loss = Money.ZERO, description = "krótko"))
        assertEquals(
            SubmitClaimResult.Invalid(
                setOf(ClaimValidationError.INCIDENT_IN_FUTURE, ClaimValidationError.LOSS_ZERO,
                      ClaimValidationError.DESCRIPTION_TOO_SHORT)
            ),
            result,
        )
    }

    @Test
    fun `incident older than reporting window is rejected`() = runTest {
        val result = submit(draft(date = today.minusDays(ClaimRules.MAX_REPORTING_DAYS + 1)))
        assertEquals(SubmitClaimResult.Invalid(setOf(ClaimValidationError.INCIDENT_TOO_OLD)), result)
    }

    @Test
    fun `repository failure is wrapped, not thrown`() = runTest {
        repo.failWith = IOException("offline")
        val result = submit(draft())
        assertTrue(result is SubmitClaimResult.Failed)
    }
}
