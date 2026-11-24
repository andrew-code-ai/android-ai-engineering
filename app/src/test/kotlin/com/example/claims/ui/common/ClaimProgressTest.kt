package com.example.claims.ui.common

import com.example.claims.domain.ClaimStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ClaimProgressTest {
    @Test
    fun `each status maps to the right step`() {
        assertEquals(ClaimProgress(0, rejected = false), ClaimProgress.of(ClaimStatus.SUBMITTED))
        assertEquals(ClaimProgress(1, rejected = false), ClaimProgress.of(ClaimStatus.IN_REVIEW))
        assertEquals(ClaimProgress(2, rejected = false), ClaimProgress.of(ClaimStatus.ACCEPTED))
    }

    @Test
    fun `rejection is the final step marked as rejected`() {
        assertEquals(ClaimProgress(2, rejected = true), ClaimProgress.of(ClaimStatus.REJECTED))
    }
}
