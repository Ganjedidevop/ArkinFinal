package com.example.arkin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arkin.data.security.PinVerificationResult
import com.example.arkin.data.security.VaultSecurityManager
import com.example.ui.theme.ArkinPrimary

private enum class PinFlowStep {
    VERIFY_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW,
    SUCCESS
}

@Composable
fun SecurePinChangeDialog(
    onDismiss: () -> Unit,
    onPinSuccessfullyChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val hasExistingPin = remember { VaultSecurityManager.isPinSet(context) }

    var currentStep by remember {
        mutableStateOf(if (hasExistingPin) PinFlowStep.VERIFY_CURRENT else PinFlowStep.ENTER_NEW)
    }

    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("secure_pin_dialog")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ArkinPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = ArkinPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (hasExistingPin) "Change Vault PIN" else "Set Vault PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Banner
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (currentStep) {
                    PinFlowStep.VERIFY_CURRENT -> {
                        // Steps 2 - 5: Verify Current PIN
                        Text(
                            text = "Step 1 of 3: Enter Current PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArkinPrimary
                        )
                        Text(
                            text = "To protect your confidential vault files, please enter your existing 4-digit PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        OutlinedTextField(
                            value = currentPinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    currentPinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Current 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_verify_current_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (currentPinInput.length != 4) {
                                        errorMessage = "Please enter all 4 digits of your current PIN."
                                        return@Button
                                    }
                                    when (val result = VaultSecurityManager.verifyCurrentPin(context, currentPinInput)) {
                                        is PinVerificationResult.Success -> {
                                            errorMessage = null
                                            currentStep = PinFlowStep.ENTER_NEW
                                        }
                                        is PinVerificationResult.Failure -> {
                                            errorMessage = "Incorrect current PIN. Attempts remaining: ${result.attemptsRemaining}."
                                            currentPinInput = ""
                                        }
                                        is PinVerificationResult.LockedOut -> {
                                            errorMessage = "Vault temporarily locked out. Try again in ${result.remainingSeconds}s."
                                            currentPinInput = ""
                                        }
                                    }
                                },
                                enabled = currentPinInput.length == 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                            ) {
                                Text("Next Step →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PinFlowStep.ENTER_NEW -> {
                        // Steps 6 - 8: Enter New PIN
                        Text(
                            text = if (hasExistingPin) "Step 2 of 3: Enter New PIN" else "Step 1 of 2: Create PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArkinPrimary
                        )
                        Text(
                            text = "Choose a secure 4-digit numeric passcode for your Private Vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    newPinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("New 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_enter_new_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (newPinInput.length != 4) {
                                        errorMessage = "PIN must be exactly 4 digits."
                                    } else {
                                        errorMessage = null
                                        currentStep = PinFlowStep.CONFIRM_NEW
                                    }
                                },
                                enabled = newPinInput.length == 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                            ) {
                                Text("Next: Confirm →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PinFlowStep.CONFIRM_NEW -> {
                        // Steps 9 - 11: Confirm New PIN
                        Text(
                            text = if (hasExistingPin) "Step 3 of 3: Confirm PIN" else "Step 2 of 2: Confirm PIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArkinPrimary
                        )
                        Text(
                            text = "Re-enter your new 4-digit PIN to ensure accurate confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    confirmPinInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Confirm 4-Digit PIN") },
                            placeholder = { Text("••••") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArkinPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_confirm_new_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    confirmPinInput = ""
                                    newPinInput = ""
                                    currentStep = PinFlowStep.ENTER_NEW
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    // Step 11: Verify match
                                    if (confirmPinInput != newPinInput) {
                                        errorMessage = "PINs do not match. Please re-enter."
                                        confirmPinInput = ""
                                        return@Button
                                    }
                                    // Step 12: Save securely
                                    val success = VaultSecurityManager.setPin(context, newPinInput)
                                    if (success) {
                                        onPinSuccessfullyChanged(newPinInput)
                                        currentStep = PinFlowStep.SUCCESS
                                    } else {
                                        errorMessage = "Failed to save new PIN. Please try again."
                                    }
                                },
                                enabled = confirmPinInput.length == 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary)
                            ) {
                                Text("Save PIN 🔒", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    PinFlowStep.SUCCESS -> {
                        // Step 12 Completion UI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "PIN Updated Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your Private Vault is secured with your new passcode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                            )
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ArkinPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
