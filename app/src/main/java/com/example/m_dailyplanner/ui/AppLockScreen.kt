package com.example.m_dailyplanner.ui

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gate shown when App Lock is enabled. Always resolves one way or another — if the device
 * has no usable biometric/device-credential setup, [onUnlock] fires immediately rather than
 * stranding the user in front of a lock screen that can never succeed on their device.
 */
@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authenticators = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            // BIOMETRIC_STRONG | DEVICE_CREDENTIAL isn't supported below API 30 — biometric-only here.
            BIOMETRIC_STRONG
        }
    }

    val promptInfo = remember(authenticators) {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock M-Daily Planner")
            .setAllowedAuthenticators(authenticators)
        // setNegativeButtonText() and DEVICE_CREDENTIAL in setAllowedAuthenticators() are
        // mutually exclusive — the device's own UI supplies a way out when combined.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            builder.setNegativeButtonText("Cancel")
        }
        builder.build()
    }

    val biometricPrompt = remember {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    errorMessage = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    // A single rejected attempt (wrong finger, etc.) — let the user retry.
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val availability = BiometricManager.from(context).canAuthenticate(authenticators)
        if (availability == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // No usable biometric or device credential is configured — never lock the user
            // out of their own data over a device-configuration gap.
            onUnlock()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "M-Daily Planner is locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verify it's you to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                errorMessage = null
                biometricPrompt.authenticate(promptInfo)
            }) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}
