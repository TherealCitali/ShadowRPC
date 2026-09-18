package dev.citali.shadowrpc.ui.component

import dev.citali.shadowrpc.ui.theme.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import dev.citali.shadowrpc.ui.theme.themeShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Section header, e.g. "General" / "Advanced Settings" in the screenshots. */
@Composable
fun PreferenceGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 40.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
fun PreferenceEntry(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    pillDescription: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .let { if (onClick != null && enabled) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                if (pillDescription) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                } else {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        } else if (pillDescription && onClick != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    PreferenceEntry(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = { ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
    )
}

/**
 * The big tinted "Enable X" card at the top of every feature page (screenshots 2–5).
 */
@Composable
fun MasterSwitchCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        shape = themeShape(28.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            ,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            Box { ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) }
        }
    }
}

/** Rounded settings group inspired by LunarTune's Discord integration page. */
@Composable
fun PreferenceCard(content: @Composable () -> Unit) {
    Surface(
        shape = themeShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            content()
        }
    }
}

/** Material switch with the reference design's checked/unchecked thumb symbols. */
@Composable
fun ExpressiveSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    if (LocalThemeTokens.current.miui) {
        top.yukonga.miuix.kmp.basic.Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        return
    }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = {
            Crossfade(targetState = checked, label = "Switch thumb") { on ->
                Icon(
                    imageVector = if (on) Icons.Rounded.DoneAll else Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

/** Closely spaced rows with rounded outer corners, like LunarTune Appearance. */
@Composable
fun GroupedPreferenceCard(
    first: Boolean = false,
    last: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalThemeTokens.current
    Surface(
        shape = RoundedCornerShape(
            topStart = tokens.corner(if (first) 28.dp else 4.dp),
            topEnd = tokens.corner(if (first) 28.dp else 4.dp),
            bottomStart = tokens.corner(if (last) 28.dp else 4.dp),
            bottomEnd = tokens.corner(if (last) 28.dp else 4.dp),
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 1.dp),
    ) { content() }
}
