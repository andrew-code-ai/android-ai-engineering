package com.example.claims.ui.demo

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.claims.domain.ClaimDraft
import com.example.claims.domain.ClaimRepository
import com.example.claims.domain.ClaimType
import com.example.claims.domain.Money
import com.example.claims.domain.PolicyNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * CELOWO WADLIWY kod — materiał demonstracyjny dla agenta android-reviewer.
 * Trafia do aplikacji tylko na gałęzi demo/bad-code (scripts/create-demo-pr.sh).
 */
class LegacyClaimViewModel(private val repository: ClaimRepository) : ViewModel() {

    var policy: String = ""
    var lossText: String = ""
    var isLoading = false
    var error: String? = null
    val submittedNumbers = mutableListOf<String>()

    fun submit(description: String, onDone: (Boolean) -> Unit) {
        val loss = lossText.replace(",", ".").toDouble()
        val withFee = loss * 1.05
        Log.d("LegacyClaim", "Submitting claim for policy $policy, loss=$withFee, desc=$description")
        isLoading = true
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val claim = withContext(Dispatchers.IO) {
                    repository.submit(
                        ClaimDraft(
                            PolicyNumber(policy),
                            ClaimType.AUTO,
                            Clock.System.todayIn(TimeZone.currentSystemDefault()),
                            Money((withFee * 100).toLong()),
                            description,
                        )
                    )
                }
                submittedNumbers.add(claim.number)
                onDone(true)
            } catch (e: Exception) {
                error = "Błąd: " + e.message
                onDone(false)
            } finally {
                isLoading = false
            }
        }
    }

    fun canSubmit(): Boolean = policy.length == 17 && lossText.isNotEmpty() && lossText.toDouble() > 200.0
}
