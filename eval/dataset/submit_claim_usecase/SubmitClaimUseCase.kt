package com.example.claims.domain

import kotlin.coroutines.cancellation.CancellationException

@JvmInline
value class PolicyNumber(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Invalid policy number format" }
    }

    companion object {
        private val PATTERN = Regex("""POL-\d{4}-\d{8}""")
    }
}

@JvmInline
value class Money(val grosze: Long) {
    init {
        require(grosze >= 0) { "Amount cannot be negative" }
    }
}

data class ClaimDraft(
    val policyNumber: PolicyNumber,
    val estimatedLoss: Money,
    val description: String
)

sealed interface SubmitClaimResult {
    data class Submitted(val claimId: String) : SubmitClaimResult
    data object PolicyInactive : SubmitClaimResult
    data class Rejected(val reason: String) : SubmitClaimResult
    data class Failed(val cause: Throwable) : SubmitClaimResult
}

interface ClaimRepository {
    suspend fun isPolicyActive(policy: PolicyNumber): Boolean
    suspend fun submit(draft: ClaimDraft): String
}

class SubmitClaimUseCase(private val repository: ClaimRepository) {

    suspend operator fun invoke(draft: ClaimDraft): SubmitClaimResult {
        if (draft.description.isBlank()) return SubmitClaimResult.Rejected("Description is required")
        return try {
            if (!repository.isPolicyActive(draft.policyNumber)) {
                SubmitClaimResult.PolicyInactive
            } else {
                SubmitClaimResult.Submitted(repository.submit(draft))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SubmitClaimResult.Failed(e)
        }
    }
}
