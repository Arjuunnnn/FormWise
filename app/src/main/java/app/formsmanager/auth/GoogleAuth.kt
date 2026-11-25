package app.formsmanager.auth

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/** Thrown when the user still has to grant access; launch [intent] and retry afterwards. */
class ConsentRequired(val intent: PendingIntent) : Exception("Google access has not been granted yet")

class GoogleAuth(context: Context) {

    private val client = Identity.getAuthorizationClient(context)

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(SCOPES.map { Scope(it) })
        .build()

    /**
     * Returns a fresh access token. Once the user has granted access this returns without
     * showing any UI, so call it before every API request instead of caching the token.
     */
    suspend fun accessToken(): String {
        val result = client.authorize(request).await()
        if (result.hasResolution()) {
            throw ConsentRequired(checkNotNull(result.pendingIntent))
        }
        return checkNotNull(result.accessToken) { "Google did not return an access token" }
    }

    private companion object {
        val SCOPES = listOf(
            "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/forms.body",
            "https://www.googleapis.com/auth/forms.responses.readonly",
        )
    }
}
