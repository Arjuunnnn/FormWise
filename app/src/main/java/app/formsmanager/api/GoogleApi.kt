package app.formsmanager.api

import android.net.Uri
import app.formsmanager.auth.GoogleAuth
import app.formsmanager.editor.FormDraft
import app.formsmanager.editor.QuestionDraft
import app.formsmanager.editor.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class FormSummary(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val createdTime: String = "",
    val starred: Boolean = false,
    val ownedByMe: Boolean = true,
    val ownerName: String = "",
)

data class UserInfo(val name: String, val email: String)

data class Collaborator(
    val id: String,
    val name: String,
    val email: String,
    val isOwner: Boolean,
)

class ApiException(val code: Int, val status: String, message: String) : Exception(message)

class GoogleApi(private val auth: GoogleAuth) {

    /**
     * Lists forms, most recently modified first, following Drive's paging up to [maxPages] pages
     * of 100. With [trashed] it lists the trash instead.
     */
    suspend fun listForms(trashed: Boolean = false, maxPages: Int = 5): List<FormSummary> {
        val all = mutableListOf<FormSummary>()
        var token: String? = null
        var pages = 0
        do {
            val builder = Uri.parse(DRIVE_FILES).buildUpon()
                .appendQueryParameter("q", "mimeType='application/vnd.google-apps.form' and trashed=$trashed")
                .appendQueryParameter(
                    "fields",
                    "nextPageToken,files(id,name,modifiedTime,createdTime,starred,ownedByMe,owners(displayName))",
                )
                .appendQueryParameter("orderBy", "modifiedTime desc")
                .appendQueryParameter("pageSize", "100")
            token?.let { builder.appendQueryParameter("pageToken", it) }
            val json = sendWithRetry("GET", builder.build().toString())
            val files = json.optJSONArray("files")
            if (files != null) {
                for (i in 0 until files.length()) {
                    val file = files.getJSONObject(i)
                    all += FormSummary(
                        id = file.getString("id"),
                        name = file.optString("name"),
                        modifiedTime = file.optString("modifiedTime"),
                        createdTime = file.optString("createdTime"),
                        starred = file.optBoolean("starred"),
                        ownedByMe = file.optBoolean("ownedByMe", true),
                        ownerName = file.optJSONArray("owners")?.optJSONObject(0)?.optString("displayName").orEmpty(),
                    )
                }
            }
            token = json.optString("nextPageToken").ifBlank { null }
            pages++
        } while (token != null && pages < maxPages)
        return all
    }

    /** Renames the file in Drive. The title respondents see is separate and is edited in the editor. */
    suspend fun renameForm(formId: String, name: String) {
        send("PATCH", "$DRIVE_FILES/$formId?fields=id", JSONObject().put("name", name))
    }

    suspend fun setStarred(formId: String, starred: Boolean) {
        send("PATCH", "$DRIVE_FILES/$formId?fields=id", JSONObject().put("starred", starred))
    }

    /** Moves the form to the Drive trash, or brings it back. Trashed forms stop accepting responses. */
    suspend fun setTrashed(formId: String, trashed: Boolean) {
        send("PATCH", "$DRIVE_FILES/$formId?fields=id", JSONObject().put("trashed", trashed))
    }

    /** Deletes the form for good. This cannot be undone. */
    suspend fun deleteForever(formId: String) {
        send("DELETE", "$DRIVE_FILES/$formId")
    }

    suspend fun getUser(): UserInfo {
        val user = sendWithRetry("GET", "$DRIVE_ABOUT?fields=user(displayName,emailAddress)").getJSONObject("user")
        return UserInfo(user.optString("displayName"), user.optString("emailAddress"))
    }

    /** People who can edit the form, owner first. Link sharing and viewers are left out. */
    suspend fun listCollaborators(formId: String): List<Collaborator> {
        val url = Uri.parse("$DRIVE_FILES/$formId/permissions").buildUpon()
            .appendQueryParameter("fields", "permissions(id,role,type,emailAddress,displayName)")
            .build()
            .toString()
        val permissions = sendWithRetry("GET", url).optJSONArray("permissions") ?: return emptyList()
        val people = mutableListOf<Collaborator>()
        for (i in 0 until permissions.length()) {
            val permission = permissions.getJSONObject(i)
            val role = permission.optString("role")
            if (permission.optString("type") != "user" || role !in EDIT_ROLES) continue
            val email = permission.optString("emailAddress")
            people += Collaborator(
                id = permission.getString("id"),
                name = permission.optString("displayName").ifBlank { email.substringBefore('@') },
                email = email,
                isOwner = role == "owner",
            )
        }
        return people.sortedByDescending { it.isOwner }
    }

    /** Shares the form as an editor and emails the person an invitation. */
    suspend fun inviteCollaborator(formId: String, email: String) {
        val url = Uri.parse("$DRIVE_FILES/$formId/permissions").buildUpon()
            .appendQueryParameter("sendNotificationEmail", "true")
            .build()
            .toString()
        val body = JSONObject().put("type", "user").put("role", "writer").put("emailAddress", email)
        send("POST", url, body)
    }

    suspend fun removeCollaborator(formId: String, permissionId: String) {
        send("DELETE", "$DRIVE_FILES/$formId/permissions/$permissionId")
    }

    suspend fun getForm(formId: String): LoadedForm = parseForm(sendWithRetry("GET", "$FORMS_BASE/$formId"))

    suspend fun createForm(title: String): LoadedForm {
        val info = JSONObject().put("title", title).put("documentTitle", title)
        return parseForm(send("POST", FORMS_BASE, JSONObject().put("info", info)))
    }

    /** Forms made through the API start unpublished, so publish them before handing out a link. */
    suspend fun setPublishSettings(formId: String, published: Boolean, accepting: Boolean) {
        val state = JSONObject().put("isPublished", published).put("isAcceptingResponses", accepting)
        val body = JSONObject().put("publishSettings", JSONObject().put("publishState", state))
        send("POST", "$FORMS_BASE/$formId:setPublishSettings", body)
    }

    suspend fun createFromTemplate(name: String, template: FormTemplate): LoadedForm {
        val created = createForm(name)
        val draft = FormDraft(title = name, questions = template.questions())
        val extra = if (template.isQuiz) listOf(quizSettingsRequest()) else emptyList()
        val saved = saveForm(created, draft, extra)
        setPublishSettings(saved.formId, published = true, accepting = true)
        return saved.copy(isPublished = true, isAcceptingResponses = true)
    }

    /**
     * Copies the form with Drive, so everything comes across: sections, images, grids, quiz
     * answer keys and settings, not only the questions this editor can show.
     */
    suspend fun duplicateForm(source: LoadedForm): LoadedForm {
        val title = "${source.draft.title} (copy)"
        val newId = send("POST", "$DRIVE_FILES/${source.formId}/copy?fields=id", JSONObject().put("name", title))
            .getString("id")
        val copied = getForm(newId)
        // Drive renames the file but not the title respondents see.
        val saved = saveForm(copied, copied.draft.copy(title = title))
        setPublishSettings(saved.formId, published = true, accepting = true)
        return saved.copy(isPublished = true, isAcceptingResponses = true)
    }

    /** Turns quiz mode on or off. Returns the form as Google now has it. */
    suspend fun setQuiz(formId: String, isQuiz: Boolean): LoadedForm = updateSettings(
        formId,
        JSONObject().put("quizSettings", JSONObject().put("isQuiz", isQuiz)),
        "quizSettings.isQuiz",
    )

    /** [type] is DO_NOT_COLLECT, VERIFIED or RESPONDER_INPUT. */
    suspend fun setEmailCollection(formId: String, type: String): LoadedForm = updateSettings(
        formId,
        JSONObject().put("emailCollectionType", type),
        "emailCollectionType",
    )

    private suspend fun updateSettings(formId: String, settings: JSONObject, mask: String): LoadedForm {
        val request = JSONObject().put(
            "updateSettings",
            JSONObject().put("settings", settings).put("updateMask", mask),
        )
        val body = JSONObject()
            .put("requests", JSONArray().put(request))
            .put("includeFormInResponse", true)
        return parseForm(send("POST", "$FORMS_BASE/$formId:batchUpdate", body).getJSONObject("form"))
    }

    /** Sends every change in one batchUpdate, guarded by the revision the user started from. */
    suspend fun saveForm(
        base: LoadedForm,
        draft: FormDraft,
        extraRequests: List<JSONObject> = emptyList(),
    ): LoadedForm {
        val requests = extraRequests + buildSaveRequests(base, draft)
        if (requests.isEmpty()) return base
        val body = JSONObject()
            .put("requests", JSONArray(requests))
            .put("includeFormInResponse", true)
            .put("writeControl", JSONObject().put("requiredRevisionId", base.revisionId))
        val response = send("POST", "$FORMS_BASE/${base.formId}:batchUpdate", body)
        return parseForm(response.getJSONObject("form"))
    }

    /** Loads one page of up to 500 responses. Pass the previous page's token to continue. */
    suspend fun listResponses(
        formId: String,
        questions: List<QuestionDraft>,
        pageToken: String? = null,
    ): FormResponsesPayload {
        val builder = Uri.parse("$FORMS_BASE/$formId/responses").buildUpon()
            .appendQueryParameter("pageSize", "500")
        pageToken?.let { builder.appendQueryParameter("pageToken", it) }
        return parseResponses(sendWithRetry("GET", builder.build().toString()), questions)
    }

    private fun quizSettingsRequest() = JSONObject().put(
        "updateSettings",
        JSONObject()
            .put("settings", JSONObject().put("quizSettings", JSONObject().put("isQuiz", true)))
            .put("updateMask", "quizSettings.isQuiz"),
    )

    private suspend fun sendWithRetry(method: String, url: String, body: JSONObject? = null, maxRetries: Int = 3): JSONObject {
        var currentDelay = 500L
        repeat(maxRetries - 1) {
            try {
                return send(method, url, body)
            } catch (e: IOException) {
                delay(currentDelay)
                currentDelay *= 2
            } catch (e: ApiException) {
                if (e.code >= 500) {
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    throw e
                }
            }
        }
        return send(method, url, body)
    }

    private suspend fun send(method: String, url: String, body: JSONObject? = null): JSONObject {
        val token = auth.accessToken()
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                // HttpURLConnection rejects PATCH, so send POST with Google's method-override header.
                if (method == "PATCH") {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    connection.requestMethod = method
                }
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val error = runCatching { JSONObject(text).getJSONObject("error") }.getOrNull()
                    throw ApiException(
                        code = code,
                        status = error?.optString("status").orEmpty(),
                        message = error?.optString("message")?.ifEmpty { null } ?: "Request failed (HTTP $code)",
                    )
                }
                JSONObject(text.ifEmpty { "{}" })
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        const val DRIVE_ABOUT = "https://www.googleapis.com/drive/v3/about"
        const val FORMS_BASE = "https://forms.googleapis.com/v1/forms"
        val EDIT_ROLES = setOf("owner", "writer", "organizer", "fileOrganizer")
    }
}
