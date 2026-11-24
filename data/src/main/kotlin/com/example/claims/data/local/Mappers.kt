package com.example.claims.data.local

import com.example.claims.domain.Claim
import com.example.claims.domain.ClaimId
import com.example.claims.domain.ClaimStatus
import com.example.claims.domain.ClaimType
import com.example.claims.domain.Money
import com.example.claims.domain.PolicyNumber
import java.time.Instant
import java.time.LocalDate

fun ClaimEntity.toDomain() = Claim(
    id = ClaimId(id),
    number = number,
    policyNumber = PolicyNumber(policyNumber),
    type = ClaimType.valueOf(type),
    incidentDate = LocalDate.ofEpochDay(incidentEpochDay),
    estimatedLoss = Money(lossGrosze),
    description = description,
    status = ClaimStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun Claim.toEntity() = ClaimEntity(
    id = id.value,
    number = number,
    policyNumber = policyNumber.value,
    type = type.name,
    incidentEpochDay = incidentDate.toEpochDay(),
    lossGrosze = estimatedLoss.grosze,
    description = description,
    status = status.name,
    createdAtMillis = createdAt.toEpochMilli(),
)
