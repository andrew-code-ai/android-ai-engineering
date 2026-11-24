package com.example.claims.ui.common

import androidx.annotation.StringRes
import com.example.claims.R
import com.example.claims.domain.ClaimStatus
import com.example.claims.domain.ClaimType

@StringRes
fun ClaimType.label(): Int = when (this) {
    ClaimType.AUTO -> R.string.type_auto
    ClaimType.HOME -> R.string.type_home
    ClaimType.TRAVEL -> R.string.type_travel
}

@StringRes
fun ClaimStatus.label(): Int = when (this) {
    ClaimStatus.SUBMITTED -> R.string.status_submitted
    ClaimStatus.IN_REVIEW -> R.string.status_in_review
    ClaimStatus.ACCEPTED -> R.string.status_accepted
    ClaimStatus.REJECTED -> R.string.status_rejected
}
