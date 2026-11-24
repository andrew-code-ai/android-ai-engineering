package com.example.claims.ui.common

import com.example.claims.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatTest {
    @Test
    fun `parses polish and dot decimals`() {
        assertEquals(Money(125_050), MoneyFormat.parse("1 250,50"))
        assertEquals(Money(125_050), MoneyFormat.parse("1250.5"))
        assertEquals(Money(100_000), MoneyFormat.parse("1000"))
    }

    @Test
    fun `rejects invalid amounts`() {
        assertNull(MoneyFormat.parse("12,345"))
        assertNull(MoneyFormat.parse("-10"))
        assertNull(MoneyFormat.parse("abc"))
    }
}
