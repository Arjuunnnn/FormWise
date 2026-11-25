package app.formsmanager

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import app.formsmanager.editor.ui.EditorActions
import app.formsmanager.editor.ui.EditorScreen
import app.formsmanager.ui.AccountScreen
import app.formsmanager.ui.CsvExport
import app.formsmanager.ui.ExportScreen
import app.formsmanager.ui.InsightsScreen
import app.formsmanager.ui.TrashScreen
import app.formsmanager.ui.csvFileName
import app.formsmanager.ui.shareCsv
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.WifiOff
import app.formsmanager.ui.BrandedLoading
import app.formsmanager.ui.GoogleAccessUrl
import app.formsmanager.ui.GoogleFormsUrl
import app.formsmanager.ui.HelpScreen
import app.formsmanager.ui.MessageScreen
import app.formsmanager.ui.OnboardingScreen
import app.formsmanager.ui.PrivacyScreen
import app.formsmanager.ui.QuestionDetailScreen
import app.formsmanager.ui.sendFeedback
import app.formsmanager.ui.AllFormsScreen
import app.formsmanager.ui.BusyOverlay
import app.formsmanager.ui.DetailActions
import app.formsmanager.ui.BottomNav
import app.formsmanager.ui.FormActions
import app.formsmanager.ui.FormDetailScreen
import app.formsmanager.ui.FormsTheme
import app.formsmanager.ui.ThemeMode
import app.formsmanager.ui.FullScreenLoading
import app.formsmanager.ui.NavItem
import app.formsmanager.ui.NewFormScreen
import app.formsmanager.ui.ResponseActions
import app.formsmanager.ui.ResponseViewerScreen
import app.formsmanager.ui.ResponsesScreen
import app.formsmanager.ui.SettingsActions
import app.formsmanager.ui.SettingsScreen
import app.formsmanager.ui.SignInScreen
import app.formsmanager.ui.TodayScreen
import app.formsmanager.ui.copyText
import app.formsmanager.ui.editLink
import app.formsmanager.ui.openUrl
import app.formsmanager.ui.responderLink
import app.formsmanager.ui.sheetLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("formwise_prefs", MODE_PRIVATE)
        setContent {
            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.System,
                )
            }
            var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
            FormsTheme(themeMode) {
                AppRoot(
                    showOnboarding = !onboarded,
                    onOnboardingDone = {
                        onboarded = true
                        prefs.edit().putBoolean("onboarded", true).apply()
                    },
                    themeMode = themeMode,
                    onThemeMode = {
                        themeMode = it
                        prefs.edit().putString("theme_mode", it.name).apply()
                    },
                )
            }
        }
    }
}

@Composable
private fun AppRoot(
    showOnboarding: Boolean,
    onOnboardingDone: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    vm: SessionViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        vm.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(state.pendingConsent) {
        state.pendingConsent?.let { intent ->
            consentLauncher.launch(IntentSenderRequest.Builder(intent).build())
            vm.consentLaunched()
        }
    }

    LaunchedEffect(Unit) {
        vm.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    BackHandler(enabled = state.auth == AuthState.SignedIn && state.routes.size > 1) { vm.back() }

    fun openSheets(formId: String) {
        val sheetId = state.data[formId]?.form?.linkedSheetId
        if (sheetId != null) {
            context.openUrl(sheetLink(sheetId))
        } else {
            vm.toast("No linked spreadsheet yet. In Google Forms, open Responses and choose Link to Sheets.")
        }
    }

    val formActions = FormActions(
        onOpenSheets = ::openSheets,
        onCopyLink = { context.copyText("Link", responderLink(it)) },
        onDuplicate = vm::duplicateForm,
        onSetAccepting = vm::setAccepting,
        onToggleStar = vm::toggleStar,
        onTrash = vm::trashForm,
    )

    // Export: the CSV is built when the user picks "Save", then written where they choose.
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val text = pendingCsv
        pendingCsv = null
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { vm.toast("Saved") }.onFailure { vm.toast("Couldn't save the file") }
        }
    }

    Box {
        when (state.auth) {
            AuthState.SignedOut -> if (showOnboarding) {
                OnboardingScreen(onDone = onOnboardingDone)
            } else {
                SignInScreen(message = state.message, onSignIn = vm::signIn)
            }
            AuthState.Loading -> BrandedLoading()
            AuthState.Failed -> MessageScreen(
                icon = Icons.Outlined.WifiOff,
                title = "Can't reach Google",
                body = state.message?.takeIf { it.isNotBlank() } ?: "Check your connection and try again.",
                primaryLabel = "Try again",
                onPrimary = vm::retry,
                secondaryLabel = "Sign in again",
                onSecondary = vm::signOut,
            )
            AuthState.SignedIn -> when (val route = state.route) {
                is Route.Main -> {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (route.tab) {
                                MainTab.Forms -> TodayScreen(
                                    userName = state.user?.name,
                                    forms = state.forms,
                                    data = state.data,
                                    syncing = state.syncing,
                                    onSeeAll = { vm.navigate(Route.AllForms) },
                                    onOpenForm = vm::openForm,
                                    onOpenResponse = vm::openResponse,
                                    onRefresh = vm::refresh,
                                    onNewForm = { vm.navigate(Route.NewForm) },
                                )
                                MainTab.Responses -> ResponsesScreen(
                                    forms = state.forms,
                                    data = state.data,
                                    syncing = state.syncing,
                                    onOpenForm = vm::openForm,
                                )
                                MainTab.Account -> AccountScreen(
                                    user = state.user,
                                    themeMode = themeMode,
                                    onThemeMode = onThemeMode,
                                    onTrash = vm::openTrash,
                                    onHelp = { vm.navigate(Route.Help) },
                                    onPrivacy = { vm.navigate(Route.Privacy) },
                                    onSignOut = vm::signOut,
                                )
                            }
                        }
                        BottomNav(
                            selected = when (route.tab) {
                                MainTab.Forms -> NavItem.Forms
                                MainTab.Responses -> NavItem.Responses
                                MainTab.Account -> NavItem.Account
                            },
                            onSelect = { item ->
                                when (item) {
                                    NavItem.Forms -> vm.selectTab(MainTab.Forms)
                                    NavItem.Responses -> vm.selectTab(MainTab.Responses)
                                    NavItem.Templates -> vm.navigate(Route.NewForm)
                                    NavItem.Account -> vm.selectTab(MainTab.Account)
                                }
                            },
                        )
                    }
                }
                Route.AllForms -> AllFormsScreen(
                    forms = state.forms,
                    data = state.data,
                    onOpenForm = vm::openForm,
                    onBack = { vm.back() },
                    onNewForm = { vm.navigate(Route.NewForm) },
                    actions = formActions,
                )
                Route.NewForm -> NewFormScreen(onBack = { vm.back() }, onCreate = vm::createForm)
                is Route.FormDetail -> {
                    val summary = state.forms.firstOrNull { it.id == route.formId }
                    if (summary != null) {
                        FormDetailScreen(
                            summary = summary,
                            data = state.data[route.formId],
                            error = state.loadErrors[route.formId],
                            actions = DetailActions(
                                onBack = { vm.back() },
                                onSettings = { vm.openSettings(route.formId) },
                                onOpenSheets = { openSheets(route.formId) },
                                onEditHere = { vm.openEditor(route.formId) },
                                onEditInGoogle = { context.openUrl(editLink(route.formId)) },
                                onOpenResponse = { vm.openResponse(route.formId, it) },
                                onOpenQuestion = { vm.navigate(Route.Question(route.formId, it)) },
                                onToggleStar = { vm.toggleStar(route.formId) },
                                onInsights = { vm.navigate(Route.Insights(route.formId)) },
                                onExport = { vm.navigate(Route.Export(route.formId)) },
                                onLoadMore = { vm.loadMoreResponses(route.formId) },
                                onRetry = { vm.loadForm(route.formId) },
                            ),
                        )
                    } else {
                        NotFound("Form not found", "It may have been deleted, or you may no longer have access to it.") { vm.back() }
                    }
                }
                is Route.Response -> {
                    val data = state.data[route.formId]
                    if (data != null && route.index in data.responses.indices) {
                        ResponseViewerScreen(
                            data = data,
                            index = route.index,
                            actions = ResponseActions(
                                onBack = { vm.back() },
                                onShow = { vm.showResponse(route.formId, it) },
                                onCopy = { context.copyText("Answers", it) },
                                onOpenSheets = { openSheets(route.formId) },
                            ),
                        )
                    } else {
                        NotFound("Response not available", "It may have been removed, or the form hasn't finished loading.") { vm.back() }
                    }
                }
                is Route.Settings -> SettingsScreen(
                    summary = state.forms.firstOrNull { it.id == route.formId },
                    data = state.data[route.formId],
                    link = state.data[route.formId]?.form?.responderUri?.ifBlank { null } ?: responderLink(route.formId),
                    shortLink = state.shortLinks[route.formId],
                    collaborators = state.collaborators[route.formId],
                    actions = SettingsActions(
                        onBack = { vm.back() },
                        onShortLink = {
                            val known = state.shortLinks[route.formId]
                            if (known != null) {
                                context.copyText("Short link", known)
                            } else {
                                val long = state.data[route.formId]?.form?.responderUri?.ifBlank { null } ?: responderLink(route.formId)
                                vm.shortenLink(route.formId, long) { context.copyText("Short link", it) }
                            }
                        },
                        onRename = { vm.renameForm(route.formId, it) },
                        onTrash = { vm.trashForm(route.formId) },
                        onQuiz = { vm.setQuiz(route.formId, it) },
                        onEmailCollection = { vm.setEmailCollection(route.formId, it) },
                        onCopyLink = { context.copyText("Link", it) },
                        onSave = { vm.setAccepting(route.formId, it) },
                        onOpenInGoogle = { context.openUrl(editLink(route.formId)) },
                        onInvite = { vm.inviteCollaborator(route.formId, it) },
                        onRemove = { vm.removeCollaborator(route.formId, it) },
                    ),
                )
                is Route.Question -> {
                    val data = state.data[route.formId]
                    val question = data?.form?.draft?.questions?.firstOrNull { it.id == route.questionId }
                    if (data != null && question != null) {
                        QuestionDetailScreen(data = data, question = question, onBack = { vm.back() })
                    } else {
                        NotFound("Question not found", "It may have been removed from the form.") { vm.back() }
                    }
                }
                Route.Trash -> TrashScreen(
                    trash = state.trash,
                    onBack = { vm.back() },
                    onRestore = vm::restoreForm,
                    onDeleteForever = vm::deleteForever,
                )
                is Route.Insights -> {
                    val data = state.data[route.formId]
                    if (data != null) {
                        InsightsScreen(data = data, onBack = { vm.back() }, onLoadMore = { vm.loadMoreResponses(route.formId) })
                    } else {
                        NotFound("Form not found", "It may have been deleted, or you may no longer have access to it.") { vm.back() }
                    }
                }
                is Route.Export -> {
                    val data = state.data[route.formId]
                    val name = state.forms.firstOrNull { it.id == route.formId }?.name ?: data?.form?.draft?.title.orEmpty()
                    if (data != null) {
                        ExportScreen(
                            data = data,
                            onBack = { vm.back() },
                            onSave = { email, score ->
                                pendingCsv = CsvExport.build(data, email, score)
                                saveCsv.launch(csvFileName(name))
                            },
                            onShare = { email, score ->
                                context.shareCsv(csvFileName(name), CsvExport.build(data, email, score))
                            },
                            onLoadMore = { vm.loadMoreResponses(route.formId) },
                        )
                    } else {
                        NotFound("Form not found", "It may have been deleted, or you may no longer have access to it.") { vm.back() }
                    }
                }
                Route.Help -> HelpScreen(
                    onBack = { vm.back() },
                    onFeedback = { context.sendFeedback() },
                    onOpenGoogleForms = { context.openUrl(GoogleFormsUrl) },
                    onPrivacy = { vm.navigate(Route.Privacy) },
                )
                Route.Privacy -> PrivacyScreen(
                    onBack = { vm.back() },
                    onManageAccess = { context.openUrl(GoogleAccessUrl) },
                )
                is Route.Editor -> EditorScreen(
                    openId = route.openId,
                    form = route.form,
                    saving = state.saving,
                    saveProblem = state.saveProblem,
                    responses = state.responses,
                    actions = EditorActions(
                        onBack = { vm.back() },
                        onSave = vm::saveForm,
                        onDismissProblem = vm::dismissSaveProblem,
                        onReload = vm::reloadEditor,
                        onLoadResponses = vm::loadResponses,
                    ),
                )
            }
        }
        state.busy?.let { BusyOverlay(it) }
    }
}

@Composable
private fun NotFound(title: String, body: String, onBack: () -> Unit) {
    MessageScreen(
        icon = Icons.Outlined.SearchOff,
        title = title,
        body = body,
        primaryLabel = "Go back",
        onPrimary = onBack,
        onBack = onBack,
    )
}
