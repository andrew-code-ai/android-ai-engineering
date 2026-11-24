package com.example.claims.ui.newclaim

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.claims.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewClaimScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsFieldErrorsFromState() {
        compose.setContent {
            NewClaimScreen(
                state = NewClaimUiState(errors = mapOf(Field.POLICY to R.string.error_policy_inactive)),
                actions = NewClaimActions(),
            )
        }
        compose.onNodeWithText(context.getString(R.string.error_policy_inactive)).assertExists()
    }

    @Test
    fun submitIsDisabledWhileSubmitting() {
        compose.setContent { NewClaimScreen(NewClaimUiState(isSubmitting = true), NewClaimActions()) }
        compose.onNodeWithTag(NewClaimTags.SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun typingAndSubmittingCallsActions() {
        val typed = mutableListOf<String>()
        var submitted = false
        compose.setContent {
            NewClaimScreen(
                state = NewClaimUiState(),
                actions = NewClaimActions(onPolicyChange = { typed += it }, onSubmit = { submitted = true }),
            )
        }
        compose.onNodeWithTag(NewClaimTags.POLICY).performTextInput("POL-2024-00012345")
        compose.onNodeWithTag(NewClaimTags.SUBMIT).assertIsEnabled().performClick()
        assertEquals("POL-2024-00012345", typed.last())
        assertTrue(submitted)
    }
}
