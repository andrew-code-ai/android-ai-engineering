package com.example.claims.data.remote

import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.PolicyNumber
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class SubmittedClaimDto(val id: String, val number: String)

/** Kontrakt API ubezpieczyciela. W demo — implementacja symulowana, bez sieci. */
interface ClaimsRemoteDataSource {
    suspend fun isPolicyActive(policy: PolicyNumber): Boolean
    suspend fun submit(draft: ClaimDraft): SubmittedClaimDto
}

/**
 * Symulacja backendu: polisy kończące się na 0 są nieaktywne, odpowiedź przychodzi z opóźnieniem.
 * Dzięki temu aplikacja działa offline, a ścieżki błędów da się przećwiczyć ręcznie.
 */
class FakeClaimsRemoteDataSource @Inject constructor() : ClaimsRemoteDataSource {
    private val sequence = AtomicInteger(0)

    override suspend fun isPolicyActive(policy: PolicyNumber): Boolean {
        delay(300)
        return !policy.value.endsWith("0")
    }

    override suspend fun submit(draft: ClaimDraft): SubmittedClaimDto {
        delay(700)
        val year = draft.incidentDate.year
        return SubmittedClaimDto(UUID.randomUUID().toString(), "SZK-%d-%06d".format(year, sequence.incrementAndGet()))
    }
}
