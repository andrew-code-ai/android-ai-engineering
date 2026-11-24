package com.example.claims.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class ClaimDto(
    val id: String,
    val pesel: String,
    val policyNumber: String,
    val amount: Double,
    val description: String
)

interface ClaimsApi {
    suspend fun submit(apiKey: String, claim: ClaimDto): String
    suspend fun fetchAll(token: String): List<ClaimDto>
}

class ClaimRepository(
    private val context: Context,
    private val api: ClaimsApi
) {
    private val apiKey = "demo-claims-prod-key-000111222333"

    private val prefs = context.getSharedPreferences("claims", Context.MODE_PRIVATE)

    suspend fun submit(claim: ClaimDto): String? = withContext(Dispatchers.IO) {
        Log.d("ClaimRepository", "Submitting claim: $claim")
        try {
            val id = api.submit(apiKey, claim)
            prefs.edit().putString("last_claim_pesel", claim.pesel).apply()
            id
        } catch (e: Exception) {
            Log.e("ClaimRepository", "submit failed", e)
            null
        }
    }

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun claimsBlocking(): List<ClaimDto> = runBlocking {
        api.fetchAll(prefs.getString("auth_token", "")!!)
    }

    fun totalPaid(claims: List<ClaimDto>): Double =
        claims.sumOf { it.amount }
}
