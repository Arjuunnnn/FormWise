package app.formsmanager.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun Context.copyText(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
}

fun responderLink(formId: String) = "https://docs.google.com/forms/d/$formId/viewform"

fun editLink(formId: String) = "https://docs.google.com/forms/d/$formId/edit"

fun sheetLink(sheetId: String) = "https://docs.google.com/spreadsheets/d/$sheetId"

const val GoogleFormsUrl = "https://docs.google.com/forms/"
const val GoogleAccessUrl = "https://myaccount.google.com/permissions"

/** Opens the user's mail app with a blank "Formwise feedback" draft. */
fun Context.sendFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "Formwise feedback")
    }
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show() }
}

fun Context.appVersion(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

/** Writes [csv] to the app cache and opens the share sheet for it. */
fun Context.shareCsv(fileName: String, csv: String) {
    runCatching {
        val dir = java.io.File(cacheDir, "exports").apply { mkdirs() }
        // Remove earlier exports so responses don't pile up in the cache.
        dir.listFiles()?.forEach { it.delete() }
        val file = java.io.File(dir, fileName)
        file.writeText(csv, Charsets.UTF_8)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Share responses"))
    }.onFailure { Toast.makeText(this, "Couldn't prepare the file", Toast.LENGTH_SHORT).show() }
}

/** A safe file name such as "Hackathon-registration.csv". */
fun csvFileName(formName: String): String {
    val base = formName.trim().replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').take(60).ifEmpty { "responses" }
    return "$base.csv"
}
