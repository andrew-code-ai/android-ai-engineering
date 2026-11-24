package com.example.claims.ui.common

import com.example.claims.domain.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Formatowanie i parsowanie kwot należy do prezentacji (CLAUDE.md). */
object MoneyFormat {
    private val PL = Locale.forLanguageTag("pl-PL")

    fun format(money: Money): String =
        NumberFormat.getCurrencyInstance(PL).format(BigDecimal.valueOf(money.grosze, 2))

    /** "1250", "1 250,5", "1250.50" → Money; null gdy format niepoprawny lub > 2 miejsc po przecinku. */
    fun parse(input: String): Money? {
        val normalized = input.replace(" ", "").replace(" ", "").replace(',', '.')
        if (!Regex("""\d{1,9}(\.\d{1,2})?""").matches(normalized)) return null
        return Money(BigDecimal(normalized).movePointRight(2).longValueExact())
    }
}
