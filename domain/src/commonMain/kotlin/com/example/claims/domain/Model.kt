package com.example.claims.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** Kwota w groszach. Nigdy Double — brak błędów zaokrągleń. */
@JvmInline
value class Money(val grosze: Long) : Comparable<Money> {
    init {
        require(grosze >= 0) { "Amount cannot be negative" }
    }

    operator fun plus(other: Money) = Money(grosze + other.grosze)
    override fun compareTo(other: Money) = grosze.compareTo(other.grosze)

    companion object {
        val ZERO = Money(0)
        fun ofZloty(zloty: Long) = Money(zloty * 100)
    }
}

/** Numer polisy w formacie POL-RRRR-NNNNNNNN (format syntetyczny projektu demo). */
@JvmInline
value class PolicyNumber(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Invalid policy number format" }
    }

    companion object {
        private val PATTERN = Regex("""POL-\d{4}-\d{8}""")
        fun parse(raw: String): PolicyNumber? =
            raw.trim().uppercase().takeIf(PATTERN::matches)?.let(::PolicyNumber)
    }
}

@JvmInline
value class ClaimId(val value: String)

enum class ClaimType { AUTO, HOME, TRAVEL }

enum class ClaimStatus { SUBMITTED, IN_REVIEW, ACCEPTED, REJECTED }

data class ClaimDraft(
    val policyNumber: PolicyNumber,
    val type: ClaimType,
    val incidentDate: LocalDate,
    val estimatedLoss: Money,
    val description: String,
)

data class Claim(
    val id: ClaimId,
    val number: String,
    val policyNumber: PolicyNumber,
    val type: ClaimType,
    val incidentDate: LocalDate,
    val estimatedLoss: Money,
    val description: String,
    val status: ClaimStatus,
    val createdAt: Instant,
)
