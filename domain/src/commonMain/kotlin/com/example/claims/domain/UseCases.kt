package com.example.claims.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.coroutines.cancellation.CancellationException

sealed interface SubmitClaimResult {
    data class Submitted(val claim: Claim) : SubmitClaimResult
    data class Invalid(val errors: Set<ClaimValidationError>) : SubmitClaimResult
    data object PolicyInactive : SubmitClaimResult
    data class Failed(val cause: Throwable) : SubmitClaimResult
}

/**
 * Bez adnotacji DI — kod wspólny dla iOS. Na Androidzie use case’y tworzy moduł Hilt w :app
 * (UseCaseModule), na iOS — zwykły konstruktor albo kontener po stronie Swift.
 */
class SubmitClaimUseCase(
    private val claims: ClaimRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(draft: ClaimDraft): SubmitClaimResult {
        val errors = ClaimRules.validate(draft, clock.todayIn(timeZone))
        if (errors.isNotEmpty()) return SubmitClaimResult.Invalid(errors)
        return try {
            if (!claims.isPolicyActive(draft.policyNumber)) {
                SubmitClaimResult.PolicyInactive
            } else {
                val claim = claims.submit(draft)
                preferences.setLastClaimType(draft.type)
                SubmitClaimResult.Submitted(claim)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SubmitClaimResult.Failed(e)
        }
    }
}

class ObserveClaimsUseCase(private val claims: ClaimRepository) {
    operator fun invoke(): Flow<List<Claim>> =
        claims.observeClaims().map { list -> list.sortedByDescending { it.createdAt } }
}
