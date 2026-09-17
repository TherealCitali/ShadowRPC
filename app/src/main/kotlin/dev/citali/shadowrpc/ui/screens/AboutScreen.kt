package dev.citali.shadowrpc.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import dev.citali.shadowrpc.BuildConfig
import dev.citali.shadowrpc.R
import dev.citali.shadowrpc.ui.component.PreferenceEntry
import dev.citali.shadowrpc.ui.component.ScreenScaffold

@Composable
fun AboutScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(title = stringResource(R.string.drawer_about), onBack = onBack) {
        PreferenceEntry(
            title = stringResource(R.string.app_name),
            description = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            icon = Icons.Outlined.Info,
        )
        PreferenceEntry(
            title = stringResource(R.string.build_information),
            description = "${BuildConfig.BUILD_COMMIT} · ${BuildConfig.BUILD_TYPE}\n${BuildConfig.BUILD_DATE}",
            icon = Icons.Outlined.Code,
        )
        PreferenceEntry(
            title = stringResource(R.string.about_source),
            description = stringResource(R.string.about_source_summary),
            icon = Icons.Outlined.Code,
            onClick = { runCatching { uriHandler.openUri("https://github.com/TherealCitali/ShadowRPC") } },
        )
        PreferenceEntry(
            title = stringResource(R.string.about_license),
            description = stringResource(R.string.about_license_summary),
            icon = Icons.Outlined.Gavel,
            onClick = { runCatching { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") } },
        )
        PreferenceEntry(
            title = stringResource(R.string.about_privacy),
            description = stringResource(R.string.about_privacy_summary),
            icon = Icons.Outlined.PrivacyTip,
            onClick = { onNavigate(Routes.PRIVACY) },
        )
        PreferenceEntry(
            title = stringResource(R.string.about_terms),
            description = stringResource(R.string.about_terms_summary),
            icon = Icons.Outlined.Description,
            onClick = { onNavigate(Routes.TERMS) },
        )
        PreferenceEntry(
            title = stringResource(R.string.about_credits),
            description = stringResource(R.string.about_credits_summary),
            icon = Icons.Outlined.Favorite,
        )
    }
}
