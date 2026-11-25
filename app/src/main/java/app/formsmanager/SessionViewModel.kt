package app.formsmanager

import android.app.Application
import android.app.PendingIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.formsmanager.api.ApiException
import app.formsmanager.api.Collaborator
import app.formsmanager.api.FormResponse
import app.formsmanager.api.FormSummary
import app.formsmanager.api.FormTemplate
import app.formsmanager.api.GoogleApi
import app.formsmanager.api.LinkShortener
import app.formsmanager.api.LoadedForm
import app.formsmanager.api.ResponsesState
import app.formsmanager.api.UserInfo
import app.formsmanager.api.validateDraft
import app.formsmanager.auth.ConsentRequired
import app.formsmanager.auth.GoogleAuth
import app.formsmanager.editor.FormDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

enum class MainTab { Forms, Responses, Account }

sealed interface Route {
    data class Main(val tab: MainTab) : Route
    data object AllForms : Route
    data object NewForm : Route
    data class FormDetail(val formId: String) : Route
    data class Response(val formId: String, val index: Int) : Route
    data class Settings(val formId: String) : Route
    data class Question(val formId: String, val questionId: String) : Route
    data object Help : Route
    data object Trash : Route
    data class Insights(val formId: String) : Route
    data class Export(val formId: String) : Route
    data object Privacy : Route

    /** [openId] changes every time the editor opens or saves, so each gets a fresh editing state. */
    data class Editor(val formId: String, val form: LoadedForm, val openId: Int) : Route
}

/** [Failed] means a returning user could not reach Google at startup; it is not the same as signed out. */
enum class AuthState { SignedOut, Loading, SignedIn, Failed }

data class FormData(
    val form: LoadedForm,
    val responses: List<FormResponse>,
    /** True when Google has more than the 500 responses we loaded. */
    val hasMore: Boolean = false,
    /** Continue loading responses from here; null when everything is loaded. */
    val nextPageToken: String? = null,
)

data class SaveProblem(val message: String, val conflict: Boolean)

data class SessionState(
    val auth: AuthState = AuthState.Loading,
    val routes: List<Route> = listOf(Route.Main(MainTab.Forms)),
    val forms: List<FormSummary> = emptyList(),
    val data: Map<String, FormData> = emptyMap(),
    val loadErrors: Map<String, String> = emptyMap(),
    val collaborators: Map<String, List<Collaborator>> = emptyMap(),
    /** Short links already created this session, by form id. */
    val shortLinks: Map<String, String> = emptyMap(),
    /** Forms in the Drive trash. Null while loading. */
    val trash: List<FormSummary>? = null,
    val user: UserInfo? = null,
    val syncing: Boolean = false,
    val busy: String? = null,
    val saving: Boolean = false,
    val saveProblem: SaveProblem? = null,
    val responses: ResponsesState = ResponsesState.Idle,
    val pendingConsent: PendingIntent? = null,
    val message: String? = null,
) {
    val route: Route get() = routes.last()
}

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val api = GoogleApi(GoogleAuth(app))
    private var openCounter = 0

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val toastChannel = Channel<String>(Channel.BUFFERED)
    val toasts = toastChannel.receiveAsFlow()

    init {
        tryRestoreSession()
    }

    // ---- sign-in and loading -------------------------------------------------------------

    fun signIn() = guarded(onError = ::failSignIn) {
        _state.update { it.copy(auth = AuthState.Loading, message = null) }
        enter()
    }

    /**
     * Called once on startup. Quietly checks whether Google already holds a grant and loads the
     * dashboard if so. When consent is still needed we fall back to the sign-in screen and never
     * launch the consent UI without an explicit tap on "Continue with Google".
     */
    private fun tryRestoreSession() {
        viewModelScope.launch {
            try {
                enter()
            } catch (e: ConsentRequired) {
                _state.update { it.copy(auth = AuthState.SignedOut) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(auth = AuthState.Failed, message = e.message) }
            }
        }
    }

    /** Try the startup check again after a connection failure. */
    fun retry() {
        _state.update { it.copy(auth = AuthState.Loading, message = null) }
        tryRestoreSession()
    }

    private suspend fun enter() {
        val forms = api.listForms()
        val user = runCatching { api.getUser() }.getOrNull()
        _state.update {
            it.copy(
                auth = AuthState.SignedIn,
                forms = forms,
                user = user,
                routes = listOf(Route.Main(MainTab.Forms)),
            )
        }
        loadFormData(forms.take(DASHBOARD_FORMS).map { it.id })
    }

    fun refresh() = guarded(onError = ::toast) {
        _state.update { it.copy(syncing = true) }
        try {
            val forms = api.listForms()
            _state.update { it.copy(forms = forms) }
            loadFormData(forms.take(DASHBOARD_FORMS).map { it.id }, force = true)
        } finally {
            _state.update { it.copy(syncing = false) }
        }
    }

    fun loadForm(formId: String) {
        viewModelScope.launch { loadOne(formId) }
    }

    fun signOut() {
        _state.value = SessionState(auth = AuthState.SignedOut)
    }

    fun onConsentResult(granted: Boolean) {
        if (granted) signIn() else failSignIn("Access to Google Forms was not granted.")
    }

    fun consentLaunched() {
        _state.update { it.copy(pendingConsent = null) }
    }

    // ---- navigation ----------------------------------------------------------------------

    fun navigate(route: Route) {
        _state.update { it.copy(routes = it.routes + route) }
    }

    fun back(): Boolean {
        if (_state.value.routes.size <= 1) return false
        _state.update { it.copy(routes = it.routes.dropLast(1), saveProblem = null, responses = ResponsesState.Idle) }
        return true
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(routes = listOf(Route.Main(tab))) }
    }

    fun openForm(formId: String) {
        if (formId !in _state.value.data) loadForm(formId)
        navigate(Route.FormDetail(formId))
    }

    fun openResponse(formId: String, index: Int) = navigate(Route.Response(formId, index))

    fun showResponse(formId: String, index: Int) {
        _state.update { it.copy(routes = it.routes.dropLast(1) + Route.Response(formId, index)) }
    }

    fun openSettings(formId: String) {
        if (formId !in _state.value.data) loadForm(formId)
        navigate(Route.Settings(formId))
        loadCollaborators(formId)
    }

    fun inviteCollaborator(formId: String, email: String) = guarded(onError = ::toast) {
        withBusy("Sending invite…") { api.inviteCollaborator(formId, email) }
        toast("Invitation sent to $email")
        loadCollaborators(formId)
    }

    fun removeCollaborator(formId: String, person: Collaborator) = guarded(onError = ::toast) {
        withBusy("Removing…") { api.removeCollaborator(formId, person.id) }
        toast("${person.name} can no longer edit this form")
        loadCollaborators(formId)
    }

    private fun loadCollaborators(formId: String) {
        viewModelScope.launch {
            val people = runCatching { api.listCollaborators(formId) }.getOrNull() ?: return@launch
            _state.update { it.copy(collaborators = it.collaborators + (formId to people)) }
        }
    }

    // ---- Drive and settings actions ------------------------------------------------------

    fun toggleStar(formId: String) {
        val current = _state.value.forms.firstOrNull { it.id == formId } ?: return
        val starred = !current.starred
        setStarredLocally(formId, starred)
        guarded(onError = { setStarredLocally(formId, !starred); toast(it) }) {
            api.setStarred(formId, starred)
        }
    }

    private fun setStarredLocally(formId: String, starred: Boolean) {
        _state.update { s -> s.copy(forms = s.forms.map { if (it.id == formId) it.copy(starred = starred) else it }) }
    }

    fun renameForm(formId: String, name: String) = guarded(onError = ::toast) {
        withBusy("Renaming…") { api.renameForm(formId, name) }
        _state.update { s -> s.copy(forms = s.forms.map { if (it.id == formId) it.copy(name = name) else it }) }
        toast("Renamed")
    }

    fun trashForm(formId: String) = guarded(onError = ::toast) {
        withBusy("Moving to trash…") { api.setTrashed(formId, true) }
        _state.update { s ->
            val routes = s.routes.filterNot { routeFormId(it) == formId }.ifEmpty { listOf(Route.Main(MainTab.Forms)) }
            s.copy(
                forms = s.forms.filterNot { it.id == formId },
                data = s.data - formId,
                routes = routes,
                trash = null,
            )
        }
        toast("Moved to trash")
    }

    /** Creates a short link for [url] (the form's public link), remembers it, and hands it to [onDone]. */
    fun shortenLink(formId: String, url: String, onDone: (String) -> Unit) = guarded(onError = ::toast) {
        val short = withBusy("Creating short link…") { LinkShortener.shorten(url) }
        _state.update { it.copy(shortLinks = it.shortLinks + (formId to short)) }
        onDone(short)
    }

    fun openTrash() {
        navigate(Route.Trash)
        loadTrash()
    }

    fun loadTrash() = guarded(onError = ::toast) {
        _state.update { it.copy(trash = null) }
        val trashed = api.listForms(trashed = true)
        _state.update { it.copy(trash = trashed) }
    }

    fun restoreForm(formId: String) = guarded(onError = ::toast) {
        withBusy("Restoring…") { api.setTrashed(formId, false) }
        _state.update { s -> s.copy(trash = s.trash?.filterNot { it.id == formId }) }
        toast("Form restored")
        refresh()
    }

    fun deleteForever(formId: String) = guarded(onError = ::toast) {
        withBusy("Deleting…") { api.deleteForever(formId) }
        _state.update { s -> s.copy(trash = s.trash?.filterNot { it.id == formId }) }
        toast("Deleted permanently")
    }

    fun setQuiz(formId: String, isQuiz: Boolean) = guarded(onError = ::toast) {
        val form = withBusy(if (isQuiz) "Turning on quiz mode…" else "Turning off quiz mode…") { api.setQuiz(formId, isQuiz) }
        applyForm(formId, form)
    }

    fun setEmailCollection(formId: String, type: String) = guarded(onError = ::toast) {
        val form = withBusy("Updating…") { api.setEmailCollection(formId, type) }
        applyForm(formId, form)
    }

    /** Fetches the next page of responses and merges it into what is already loaded. */
    fun loadMoreResponses(formId: String) = guarded(onError = ::toast) {
        val existing = _state.value.data[formId] ?: return@guarded
        val token = existing.nextPageToken ?: return@guarded
        val payload = withBusy("Loading more responses…") {
            api.listResponses(formId, existing.form.draft.questions, token)
        }
        _state.update { s ->
            val current = s.data[formId] ?: return@update s
            val merged = (current.responses + payload.responses)
                .distinctBy { it.id }
                .sortedByDescending { it.submittedAt }
            s.copy(data = s.data + (formId to current.copy(responses = merged, hasMore = payload.hasMore, nextPageToken = payload.nextPageToken)))
        }
    }

    private fun applyForm(formId: String, form: LoadedForm) {
        _state.update { s ->
            val existing = s.data[formId] ?: return@update s
            s.copy(data = s.data + (formId to existing.copy(form = form)))
        }
    }

    private fun routeFormId(route: Route): String? = when (route) {
        is Route.FormDetail -> route.formId
        is Route.Response -> route.formId
        is Route.Settings -> route.formId
        is Route.Question -> route.formId
        is Route.Insights -> route.formId
        is Route.Export -> route.formId
        is Route.Editor -> route.formId
        else -> null
    }

    // ---- form actions --------------------------------------------------------------------

    fun openEditor(formId: String) = guarded(onError = ::toast) {
        val form = withBusy("Opening editor…") { api.getForm(formId) }
        navigate(Route.Editor(formId, form, ++openCounter))
    }

    fun saveForm(draft: FormDraft) {
        val current = _state.value.route as? Route.Editor ?: return
        val invalid = validateDraft(draft, current.form.isQuiz)
        if (invalid != null) {
            _state.update { it.copy(saveProblem = SaveProblem(invalid, conflict = false)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val saved = api.saveForm(current.form, draft)
                _state.update {
                    it.copy(
                        saving = false,
                        routes = it.routes.dropLast(1) + Route.Editor(current.formId, saved, ++openCounter),
                    )
                }
                loadOne(current.formId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val conflict = e.status == "FAILED_PRECONDITION" ||
                    e.message.orEmpty().contains("revision", ignoreCase = true)
                _state.update { it.copy(saving = false, saveProblem = SaveProblem(e.message.orEmpty(), conflict)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, saveProblem = SaveProblem(e.message ?: "Couldn't save the form.", conflict = false))
                }
            }
        }
    }

    fun dismissSaveProblem() {
        _state.update { it.copy(saveProblem = null) }
    }

    fun loadResponses() {
        val current = _state.value.route as? Route.Editor ?: return
        viewModelScope.launch {
            _state.update { it.copy(responses = ResponsesState.Loading) }
            val result = try {
                val payload = api.listResponses(current.formId, current.form.draft.questions)
                ResponsesState.Loaded(payload.responses, payload.hasMore)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ResponsesState.Failed(e.message ?: "Couldn't load responses.")
            }
            _state.update { it.copy(responses = result) }
        }
    }

    fun reloadEditor() {
        val current = _state.value.route as? Route.Editor ?: return
        dismissSaveProblem()
        guarded(onError = ::toast) {
            val form = withBusy("Reloading…") { api.getForm(current.formId) }
            _state.update {
                it.copy(routes = it.routes.dropLast(1) + Route.Editor(current.formId, form, ++openCounter))
            }
        }
    }

    fun setAccepting(formId: String, accepting: Boolean) = guarded(onError = ::toast) {
        withBusy("Updating…") { api.setPublishSettings(formId, published = true, accepting = accepting) }
        _state.update { s ->
            val existing = s.data[formId] ?: return@update s
            val form = existing.form.copy(isPublished = true, isAcceptingResponses = accepting)
            s.copy(data = s.data + (formId to existing.copy(form = form)))
        }
        toast(if (accepting) "Now accepting responses" else "Stopped accepting responses")
    }

    fun duplicateForm(formId: String) = guarded(onError = ::toast) {
        val source = _state.value.data[formId]?.form ?: api.getForm(formId)
        val copy = withBusy("Duplicating form…") { api.duplicateForm(source) }
        addCreated(copy)
        toast("Form duplicated")
    }

    fun createForm(name: String, template: FormTemplate) = guarded(onError = ::toast) {
        val form = withBusy("Creating form…") { api.createFromTemplate(name, template) }
        addCreated(form)
        navigate(Route.FormDetail(form.formId))
    }

    fun toast(message: String) {
        toastChannel.trySend(message)
    }

    // ---- internals -----------------------------------------------------------------------

    private fun addCreated(form: LoadedForm) {
        val summary = FormSummary(form.formId, form.draft.title, Instant.now().toString())
        _state.update {
            it.copy(
                forms = listOf(summary) + it.forms.filterNot { f -> f.id == summary.id },
                data = it.data + (form.formId to FormData(form, emptyList())),
                routes = listOf(Route.Main(MainTab.Forms)),
            )
        }
    }

    private suspend fun loadFormData(ids: List<String>, force: Boolean = false) = coroutineScope {
        val gate = Semaphore(PARALLEL_LOADS)
        ids.filter { force || it !in _state.value.data }
            .map { id -> async { gate.withPermit { loadOne(id) } } }
            .awaitAll()
    }

    private suspend fun loadOne(formId: String) {
        try {
            val form = api.getForm(formId)
            val payload = api.listResponses(formId, form.draft.questions)
            _state.update {
                it.copy(
                    data = it.data + (formId to FormData(form, payload.responses, payload.hasMore, payload.nextPageToken)),
                    loadErrors = it.loadErrors - formId,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loadErrors = it.loadErrors + (formId to (e.message ?: "Couldn't load this form"))) }
        }
    }

    private suspend fun <T> withBusy(message: String, block: suspend () -> T): T {
        _state.update { it.copy(busy = message) }
        try {
            return block()
        } finally {
            _state.update { it.copy(busy = null) }
        }
    }

    private fun failSignIn(message: String) {
        _state.update { it.copy(auth = AuthState.SignedOut, message = message) }
    }

    private fun guarded(onError: (String) -> Unit, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: ConsentRequired) {
                _state.update { it.copy(pendingConsent = e.intent) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Something went wrong")
            }
        }
    }

    private companion object {
        const val DASHBOARD_FORMS = 12
        const val PARALLEL_LOADS = 4
    }
}
