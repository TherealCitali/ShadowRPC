package dev.citali.shadowrpc.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.citali.shadowrpc.R

@Composable
fun IconUploadConsent(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.icon_consent_title)) },
        text = { Text(stringResource(R.string.icon_consent_body)) },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(R.string.icon_consent_allow)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.icon_consent_without)) } },
    )
}
