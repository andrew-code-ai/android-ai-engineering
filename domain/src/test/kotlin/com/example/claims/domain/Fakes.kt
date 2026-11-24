package com.example.claims.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeClaimRepository(
    private val activePolicies: Set<String> = setOf(ACTIVE_POLICY),
    var failWith: Exception? = null,
) : ClaimRepository {
    private val store = MutableStateFlow<List<Claim>>(emptyList())
    val submitted = mutableListOf<ClaimDraft>()

    override fun observeClaims(): Flow<List<Claim>> = store

    override suspend fun isPolicyActive(policy: PolicyNumber) = policy.value in activePolicies

    override suspend fun submit(draft: ClaimDraft): Claim {
        failWith?.let { throw it }
        submitted += draft
        val n = submitted.size
        val claim = Claim(
            id = ClaimId("id-$n"), number = "SZK-2026-%06d".format(n), policyNumber = draft.policyNumber,
            type = draft.type, incidentDate = draft.incidentDate, estimatedLoss = draft.estimatedLoss,
            description = draft.description, status = ClaimStatus.SUBMITTED, createdAt = Instant.ofEpochSecond(n.toLong()),
        )
        store.update { it + claim }
        return claim
    }

    companion object {
        const val ACTIVE_POLICY = "POL-2024-00012345"
    }
}

class FakePreferencesRepository : PreferencesRepository {
    override val lastClaimType = MutableStateFlow(ClaimType.AUTO)
    override suspend fun setLastClaimType(type: ClaimType) { lastClaimType.value = type }
}
