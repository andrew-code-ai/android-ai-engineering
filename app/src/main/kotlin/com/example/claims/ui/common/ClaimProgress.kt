package com.example.claims.ui.common

import com.example.claims.domain.ClaimStatus

/**
 * Stan paska postępu likwidacji — czysta logika, testowana na JVM bez Androida.
 * Kroki: 0 = przyjęta, 1 = w likwidacji, 2 = decyzja (uznana lub odrzucona).
 */
data class ClaimProgress(val reachedStep: Int, val rejected: Boolean) {
    companion object {
        const val STEPS = 3

        fun of(status: ClaimStatus) = when (status) {
            ClaimStatus.SUBMITTED -> ClaimProgress(reachedStep = 0, rejected = false)
            ClaimStatus.IN_REVIEW -> ClaimProgress(reachedStep = 1, rejected = false)
            ClaimStatus.ACCEPTED -> ClaimProgress(reachedStep = 2, rejected = false)
            ClaimStatus.REJECTED -> ClaimProgress(reachedStep = 2, rejected = true)
        }
    }
}
