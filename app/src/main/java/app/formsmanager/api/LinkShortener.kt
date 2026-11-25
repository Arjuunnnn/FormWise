package app.formsmanager.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Makes a short link with is.gd, which is free and needs no account.
 *
 * Google only creates forms.gle links inside the Forms editor and Apps Script. The Forms API this
 * app uses has no such call, so this is the one request in the app that does not go to Google.
 * Only the form's public responder link is sent, and only when the user asks for a short link.
 */
object LinkShortener {

    suspend fun shorten(url: String): String = withContext(Dispatchers.IO) {
        val endpoint = "https://is.gd/create.php?format=json&url=" + URLEncoder.encode(url, "UTF-8")
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Formwise (Android)")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            val short = json?.optString("shorturl").orEmpty()
            // Only accept an is.gd link back, never whatever a response happens to contain.
            if (short.startsWith("https://is.gd/")) {
                short
            } else {
                throw ApiException(
                    code,
                    "SHORTENER",
                    json?.optString("errormessage")?.ifBlank { null } ?: "Couldn't create a short link. Try again later.",
                )
            }
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Couldn't reach the link shortener. Check your connection and try again.")
        } finally {
            connection.disconnect()
        }
    }
}
