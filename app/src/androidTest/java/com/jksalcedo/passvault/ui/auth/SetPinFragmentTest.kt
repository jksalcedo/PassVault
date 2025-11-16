package com.jksalcedo.passvault.ui.auth

import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jksalcedo.passvault.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetPinFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            val fragment = SetPinDialog()
            fragment.show(activity.supportFragmentManager, "SetPinFragment")
        }
    }

    @Test

    fun testSuccessfulPinSet() {
        onView(withId(R.id.etNewPin)).perform(typeText("1234"))
        onView(withId(R.id.etConfirmPin)).perform(typeText("1234"))
        onView(ViewMatchers.withText("Save")).perform(click())

        // Dialog should be dismissed
        onView(withId(R.id.etNewPin)).check(doesNotExist())
    }

    @Test
    fun testPinMismatch() {
        onView(withId(R.id.etNewPin)).perform(typeText("1234"))
        onView(withId(R.id.etConfirmPin)).perform(typeText("5678"))
        onView(ViewMatchers.withText("Save")).perform(click())

        // Error should be displayed
        onView(withId(R.id.etConfirmPin)).check(matches(hasErrorText("PINs do not match")))
    }

    @Test
    fun testShortPin() {
        onView(withId(R.id.etNewPin)).perform(typeText("123"))
        onView(ViewMatchers.withText("Save")).perform(click())

        // Error should be displayed
        onView(withId(R.id.etNewPin)).check(matches(hasErrorText("PIN must be at least 4 digits and 6 digits at most")))
    }

    private fun hasErrorText(expectedError: String): Matcher<View> {
        return object : BoundedMatcher<View, EditText>(EditText::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("has error text: ")?.appendValue(expectedError)
            }

            override fun matchesSafely(item: EditText?): Boolean {
                return item?.error?.toString() == expectedError
            }
        }
    }
}
