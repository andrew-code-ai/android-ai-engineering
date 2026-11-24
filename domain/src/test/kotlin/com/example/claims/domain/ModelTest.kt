package com.example.claims.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelTest {
    @Test
    fun `policy number is normalised and validated`() {
        assertEquals("POL-2024-00012345", PolicyNumber.parse(" pol-2024-00012345 ")?.value)
        assertNull(PolicyNumber.parse("POL-24-123"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `money cannot be negative`() {
        Money(-1)
    }

    @Test
    fun `money adds in grosze`() {
        assertEquals(Money(1_050), Money.ofZloty(10) + Money(50))
    }
}
