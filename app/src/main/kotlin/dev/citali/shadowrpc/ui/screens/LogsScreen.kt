package dev.citali.shadowrpc.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.ui.component.ScreenScaffold
import dev.citali.shadowrpc.util.InMemoryLogTree

@Composable
fun LogsScreen(onBack: () -> Unit) {
    val lines by InMemoryLogTree.lines.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ScreenScaffold(
        title = stringResource(R.string.drawer_logs),
        onBack = onBack,
        scrollable = false,
        actions = {
            IconButton(onClick = {
                val text = lines.joinToString("\n") { "${it.time} ${it.tag ?: "-"}: ${it.message}" }
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("ShadowRPC logs", text))
            }) { Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.logs_copy)) }
            IconButton(onClick = InMemoryLogTree::clear) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.logs_clear))
            }
        },
    ) {
        if (lines.isEmpty()) {
            Text(
                stringResource(R.string.logs_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(lines) { line ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            "${line.time}  ${line.tag ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                when (line.priority) {
                                    Log.ERROR -> MaterialTheme.colorScheme.error
                                    Log.WARN -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        Text(line.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
