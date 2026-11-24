package com.example.claims.data

import com.example.claims.data.di.IoDispatcher
import com.example.claims.data.local.ClaimDao
import com.example.claims.data.local.toDomain
import com.example.claims.data.local.toEntity
import com.example.claims.data.remote.ClaimsRemoteDataSource
import com.example.claims.domain.Claim
import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.ClaimId
import com.example.claims.domain.ClaimRepository
import com.example.claims.domain.ClaimStatus
import com.example.claims.domain.PolicyNumber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

/** Room jest źródłem prawdy: UI obserwuje bazę, sieć tylko ją aktualizuje. */
class ClaimRepositoryImpl @Inject constructor(
    private val dao: ClaimDao,
    private val remote: ClaimsRemoteDataSource,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ClaimRepository {

    override fun observeClaims(): Flow<List<Claim>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun isPolicyActive(policy: PolicyNumber): Boolean =
        withContext(io) { remote.isPolicyActive(policy) }

    override suspend fun submit(draft: ClaimDraft): Claim = withContext(io) {
        val dto = remote.submit(draft)
        val claim = Claim(
            id = ClaimId(dto.id),
            number = dto.number,
            policyNumber = draft.policyNumber,
            type = draft.type,
            incidentDate = draft.incidentDate,
            estimatedLoss = draft.estimatedLoss,
            description = draft.description,
            status = ClaimStatus.SUBMITTED,
            createdAt = clock.instant(),
        )
        dao.upsert(claim.toEntity())
        claim
    }
}
