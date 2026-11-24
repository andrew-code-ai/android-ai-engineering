package com.example.claims.ui.common

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.claims.R
import com.example.claims.domain.ClaimStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Espresso: widok z XML się renderuje, ma wysokość i dostępny opis statusu. */
@RunWith(AndroidJUnit4::class)
class ClaimProgressViewTest {

    @Test
    fun inflatedFromXmlAndDescribesStatus() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(R.layout.view_claim_progress)
                activity.findViewById<ClaimProgressView>(R.id.claim_progress).status = ClaimStatus.REJECTED
            }

            onView(withId(R.id.claim_progress))
                .check(matches(isDisplayed()))
                .check(matches(withContentDescription("Status zgłoszenia: Odrzucona")))

            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<ClaimProgressView>(R.id.claim_progress).height > 0)
            }
        }
    }
}
