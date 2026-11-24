package com.example.claims.domain

import kotlinx.coroutines.flow.Flow

/** Port — implementacja w module :data. */
interface ClaimRepository {
    fun observeClaims(): Flow<List<Claim>>
    suspend fun isPolicyActive(policy: PolicyNumber): Boolean
    suspend fun submit(draft: ClaimDraft): Claim
}

/** Preferencje użytkownika — tylko dane niewrażliwe (klasa B wg AI-usage-policy.md). */
interface PreferencesRepository {
    val lastClaimType: Flow<ClaimType>
    suspend fun setLastClaimType(type: ClaimType)
}
