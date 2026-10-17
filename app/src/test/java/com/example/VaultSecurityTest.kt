package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.arkin.data.security.PinVerificationResult
import com.example.arkin.data.security.VaultSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultSecurityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clear prefs
        context.getSharedPreferences("arkin_vault_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testSetAndVerifyPin() {
        assertFalse("PIN should not be set initially", VaultSecurityManager.isPinSet(context))

        // Set valid PIN
        val success = VaultSecurityManager.setPin(context, "4826")
        assertTrue("Setting 4-digit PIN should succeed", success)
        assertTrue("PIN should now be reported as set", VaultSecurityManager.isPinSet(context))

        // Verify correct PIN
        val correctResult = VaultSecurityManager.verifyCurrentPin(context, "4826")
        assertTrue("Correct PIN must succeed", correctResult is PinVerificationResult.Success)

        // Verify incorrect PIN
        val wrongResult = VaultSecurityManager.verifyCurrentPin(context, "9999")
        assertTrue("Wrong PIN must fail", wrongResult is PinVerificationResult.Failure)
        assertEquals(4, (wrongResult as PinVerificationResult.Failure).attemptsRemaining)
    }

    @Test
    fun testInvalidPinRejection() {
        assertFalse(VaultSecurityManager.setPin(context, "12"))
        assertFalse(VaultSecurityManager.setPin(context, "12345"))
        assertFalse(VaultSecurityManager.setPin(context, "abcd"))
    }

    @Test
    fun testChangePinFlow() {
        VaultSecurityManager.setPin(context, "1111")

        // Wrong current PIN should fail change
        val wrongChange = VaultSecurityManager.changePin(context, "0000", "2222")
        assertTrue("Change PIN with wrong current PIN must fail", wrongChange.isFailure)

        // Correct current PIN should succeed
        val correctChange = VaultSecurityManager.changePin(context, "1111", "2222")
        assertTrue("Change PIN with correct current PIN must succeed", correctChange.isSuccess)

        // Verify old PIN no longer works
        val oldVerify = VaultSecurityManager.verifyCurrentPin(context, "1111")
        assertTrue("Old PIN must no longer verify", oldVerify is PinVerificationResult.Failure)

        // Verify new PIN works
        val newVerify = VaultSecurityManager.verifyCurrentPin(context, "2222")
        assertTrue("New PIN must verify successfully", newVerify is PinVerificationResult.Success)
    }
}
