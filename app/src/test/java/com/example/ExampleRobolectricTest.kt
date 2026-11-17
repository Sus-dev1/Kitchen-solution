package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.clearSessionFlag
import com.example.auth.markSessionActive
import com.example.auth.shouldKeepSignedIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kitchen Solution", appName)
  }

  @Test
  fun `session flag persists after login and clears on sign out`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    clearSessionFlag(context)
    assertFalse(shouldKeepSignedIn(context))

    markSessionActive(context)
    assertTrue(shouldKeepSignedIn(context))

    clearSessionFlag(context)
    assertFalse(shouldKeepSignedIn(context))
  }
}
