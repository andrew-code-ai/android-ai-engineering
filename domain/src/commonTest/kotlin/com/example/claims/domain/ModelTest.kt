package com.example.claims.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ModelTest {
    @Test
    fun policyNumberIsNormalisedAndValidated() {
        assertEquals("POL-2024-00012345", PolicyNumber.parse(" pol-2024-00012345 ")?.value)
        assertNull(PolicyNumber.parse("POL-24-123"))
    }

    @Test
    fun moneyCannotBeNegative() {
        assertFailsWith<IllegalArgumentException> { Money(-1) }
    }

    @Test
    fun moneyAddsInGrosze() {
        assertEquals(Money(1_050), Money.ofZloty(10) + Money(50))
    }
}
