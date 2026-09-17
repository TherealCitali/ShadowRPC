package dev.citali.shadowrpc.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled policy text: readable offline, selectable, without a WebView or tracking. */
@Composable
fun LegalScreen(privacy: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val failure = stringResource(R.string.legal_load_error)
    val loading = stringResource(R.string.legal_loading)
    val text by produceState(loading, privacy, failure) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open("legal/${if (privacy) "privacy" else "terms"}.md")
                    .bufferedReader().use { it.readText() }
            } catch (_: java.io.IOException) {
                failure
            }
        }
    }
    ScreenScaffold(
        title = stringResource(if (privacy) R.string.about_privacy else R.string.about_terms),
        onBack = onBack,
    ) {
        SelectionContainer {
            Column(Modifier.padding(horizontal = 24.dp)) {
                text.split("\n\n").filterNot { it.startsWith("# ") }.forEach { paragraph ->
                    val heading = paragraph.startsWith("## ")
                    Text(
                        text = paragraph.removePrefix("## ").trim(),
                        style = if (heading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        color = if (heading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = if (heading) 20.dp else 8.dp, bottom = 4.dp),
                    )
                }
            }
        }
    }
}
