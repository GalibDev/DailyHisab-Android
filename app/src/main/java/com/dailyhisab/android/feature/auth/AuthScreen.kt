package com.dailyhisab.android.feature.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.dailyhisab.android.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(contentPadding: PaddingValues, state: AuthUiState, viewModel: AuthViewModel, onBack: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(if (register) "Create account" else "Sign in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(if (register) "Sync your Daily Hisab profile across devices." else "Welcome back to Daily Hisab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (register) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Filled.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        PasswordField(password, { password = it }, "Password", visible, { visible = !visible })
        if (register) PasswordField(confirm, { confirm = it }, "Confirm password", visible, { visible = !visible })
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = {
                viewModel.clearNotice()
                if (register) {
                    when {
                        name.isBlank() -> viewModel.reportError(IllegalArgumentException("Enter your name"))
                        password != confirm -> viewModel.reportError(IllegalArgumentException("Passwords do not match"))
                        else -> viewModel.register(name, email, password)
                    }
                } else viewModel.signIn(email, password)
            },
            modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
        ) { Text(if (register) "Create account" else "Sign in") }
        OutlinedButton(
            onClick = {
                scope.launch {
                    runCatching {
                        val option = GetGoogleIdOption.Builder()
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                        val credential = CredentialManager.create(context).getCredential(context as Activity, request).credential
                        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    }.onSuccess(viewModel::signInWithGoogle).onFailure(viewModel::reportError)
                }
            },
            modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
        ) { Text("Continue with Google") }
        if (!register) TextButton(onClick = { viewModel.sendPasswordReset(email) }, enabled = !state.busy) { Text("Forgot password?") }
        TextButton(onClick = { register = !register; viewModel.clearNotice() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (register) "Already have an account? Sign in" else "New here? Create account")
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun PasswordField(value: String, change: (String) -> Unit, label: String, visible: Boolean, toggle: () -> Unit) {
    OutlinedTextField(
        value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton(onClick = toggle) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password") } },
    )
}
