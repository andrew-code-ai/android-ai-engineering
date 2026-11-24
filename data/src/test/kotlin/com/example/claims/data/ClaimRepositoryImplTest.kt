package com.example.claims.data

import com.example.claims.data.local.ClaimDao
import com.example.claims.data.local.ClaimEntity
import com.example.claims.data.remote.ClaimsRemoteDataSource
import com.example.claims.data.remote.SubmittedClaimDto
import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.ClaimType
import com.example.claims.domain.Money
import com.example.claims.domain.PolicyNumber
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class ClaimRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = mockk<ClaimDao>(relaxed = true)
    private val remote = mockk<ClaimsRemoteDataSource>()
    private val now = Instant.parse("2026-11-24T10:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = this@ClaimRepositoryImplTest.now
    }
    private val repo = ClaimRepositoryImpl(dao, remote, clock, dispatcher)

    @Test
    fun `submit stores claim returned by API in local database`() = runTest(dispatcher) {
        coEvery { remote.submit(any()) } returns SubmittedClaimDto("id-1", "SZK-2026-000001")
        val saved = slot<ClaimEntity>()
        coEvery { dao.upsert(capture(saved)) } returns Unit

        val claim = repo.submit(
            ClaimDraft(PolicyNumber("POL-2024-00012345"), ClaimType.AUTO, LocalDate(2026, 11, 20),
                       Money(123_45), "Uszkodzone lusterko na parkingu pod sklepem")
        )

        assertEquals("SZK-2026-000001", claim.number)
        assertEquals(12_345L, saved.captured.lossGrosze)
        assertEquals(now.toEpochMilliseconds(), saved.captured.createdAtMillis)
        coVerify(exactly = 1) { dao.upsert(any()) }
    }
}
