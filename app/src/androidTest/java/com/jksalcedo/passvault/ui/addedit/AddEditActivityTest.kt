package com.jksalcedo.passvault.ui.addedit

import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.data.local.entity.PasswordEntry
import com.jksalcedo.passvault.presentation.addedit.AddEditActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEditActivityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val intentsTestRule = IntentsTestRule(AddEditActivity::class.java, true, false)

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Initialize Espresso-Intents
        Intents.init()
    }

    @After
    fun teardown() {
        Intents.release()
    }

    @Test
    fun testAddMode_fieldsAreEmpty() {
        val intent = AddEditActivity.createIntent(context)
        val scenario = ActivityScenario.launch<AddEditActivity>(intent)

        // Verify fields are empty
        onView(withId(R.id.etTitle)).check(matches(withText("")))
        onView(withId(R.id.etUsername)).check(matches(withText("")))
        onView(withId(R.id.etPassword)).check(matches(withText("")))
        onView(withId(R.id.etNotes)).check(matches(withText("")))

        scenario.close()
    }

    @Test
    fun testEditMode_fieldsArePopulated() {
        // dummy entry to pass via intent
        val entry = PasswordEntry(
            id = 1, // ID is needed for edit mode
            title = "Old Title",
            username = "Old Username",
            passwordCipher = "cipher",
            passwordIv = "iv",
            notes = "Old Notes"
        )

        // Launch activity in edit mode
        val intent = AddEditActivity.createIntent(context, entry)
        val scenario = ActivityScenario.launch<AddEditActivity>(intent)

        // Verify fields are populated
        onView(withId(R.id.etTitle)).check(matches(withText("Old Title")))
        onView(withId(R.id.etUsername)).check(matches(withText("Old Username")))
        onView(withId(R.id.etNotes)).check(matches(withText("Old Notes")))
        onView(withId(R.id.etPassword)).check(matches(withText("")))

        scenario.close()
    }

    @Test
    fun testSaveWithEmptyTitle_showsError() {
        val intent = AddEditActivity.createIntent(context)
        val scenario = ActivityScenario.launch<AddEditActivity>(intent)

        // Leave title empty
        onView(withId(R.id.etPassword)).perform(replaceText("Test Password"))
        onView(withId(R.id.btnSave)).perform(click())

        // error is shown on the title's TextInputLayout
        onView(withId(R.id.etTitle)).check(matches(hasErrorText("Title cannot be empty!")))

        scenario.close()
    }

    @Test
    fun testSaveWithEmptyPassword_showsError() {
        val intent = AddEditActivity.createIntent(context)
        val scenario = ActivityScenario.launch<AddEditActivity>(intent)

        // Leave password empty
        onView(withId(R.id.etTitle)).perform(replaceText("Test Title"))
        onView(withId(R.id.btnSave)).perform(click())

        // error is shown on the password's TextInputLayout
        onView(withId(R.id.etPassword)).check(matches(hasErrorText("Password cannot be empty!")))

        scenario.close()
    }

    @Test
    fun testSaveWithValidData_finishesActivity() {
        // Set up a "result" for the activity to return
        //val resultData = Intent()
        //val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        //Intents.intending(IntentMatchers.anyIntent()).respondWith(result)

        val intent = AddEditActivity.createIntent(context)
        val scenario = ActivityScenario.launch<AddEditActivity>(intent)

        // Enter valid data
        onView(withId(R.id.etTitle)).perform(replaceText("Test Title"))
        onView(withId(R.id.etUsername)).perform(replaceText("Test Username"))
        onView(withId(R.id.etPassword)).perform(replaceText("Test Password"))

        // Save
        onView(withId(R.id.btnSave)).perform(click())

        // Verify that the activity finished
        assert(scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED)

        scenario.close()
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