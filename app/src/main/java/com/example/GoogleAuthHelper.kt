package com.example

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleAuthHelper {

    // Server Web Client ID from google-services.json
    private const val WEB_CLIENT_ID = "811088017095-web.apps.googleusercontent.com"

    suspend fun launchGoogleSignIn(
        context: Context,
        onSuccess: (idToken: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onSuccess(googleIdTokenCredential.idToken)
                } else {
                    onError("Unrecognized credential response")
                }
            } catch (e: GetCredentialException) {
                if (e.message?.contains("canceled", ignoreCase = true) == true) {
                    // User canceled sign-in flow intentionally
                    return@withContext
                }
                onError(e.message ?: "Google Sign-In failed")
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }
}
