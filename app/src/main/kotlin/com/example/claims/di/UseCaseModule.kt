package com.example.claims.di

import com.example.claims.domain.ClaimRepository
import com.example.claims.domain.ObserveClaimsUseCase
import com.example.claims.domain.PreferencesRepository
import com.example.claims.domain.SubmitClaimUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/** Domena jest multiplatformowa (bez javax.inject), więc use case’y składamy tutaj. */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun submitClaim(
        claims: ClaimRepository,
        preferences: PreferencesRepository,
        clock: Clock,
        timeZone: TimeZone,
    ) = SubmitClaimUseCase(claims, preferences, clock, timeZone)

    @Provides
    fun observeClaims(claims: ClaimRepository) = ObserveClaimsUseCase(claims)
}
