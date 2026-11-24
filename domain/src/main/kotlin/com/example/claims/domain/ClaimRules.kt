package com.example.claims.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ClaimValidationError { INCIDENT_IN_FUTURE, INCIDENT_TOO_OLD, LOSS_ZERO, DESCRIPTION_TOO_SHORT }

/** Reguły biznesowe zgłoszenia szkody — w domenie, nie w ViewModelu. */
object ClaimRules {
    const val MAX_REPORTING_DAYS = 3 * 365L
    const val MIN_DESCRIPTION_LENGTH = 20

    fun validate(draft: ClaimDraft, today: LocalDate): Set<ClaimValidationError> = buildSet {
        if (draft.incidentDate.isAfter(today)) add(ClaimValidationError.INCIDENT_IN_FUTURE)
        if (ChronoUnit.DAYS.between(draft.incidentDate, today) > MAX_REPORTING_DAYS) add(ClaimValidationError.INCIDENT_TOO_OLD)
        if (draft.estimatedLoss == Money.ZERO) add(ClaimValidationError.LOSS_ZERO)
        if (draft.description.trim().length < MIN_DESCRIPTION_LENGTH) add(ClaimValidationError.DESCRIPTION_TOO_SHORT)
    }
}
