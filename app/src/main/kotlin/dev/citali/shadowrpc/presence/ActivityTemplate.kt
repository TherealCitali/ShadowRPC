package dev.citali.shadowrpc.presence

import android.content.Context
import dev.citali.shadowrpc.data.Prefs
import dev.citali.shadowrpc.data.dataStore
import kotlinx.coroutines.flow.first

/** Where a presence text line takes its value from; mirrors LunarTune's ActivitySource. */
enum class ActivitySource {
    /** Label of the detected app (or the user's per-app display name). */
    APP,

    /** Literally "ShadowRPC". */
    SHADOWRPC,

    /** Android package name. */
    PACKAGE,

    /** Play Store category of the app: Game, Social, Video, ... */
    CATEGORY,

    /** User template with {app} / {package} / {category} placeholders. */
    CUSTOM,

    /** Hide this line (details / state only). */
    NONE,
    ;

    companion object {
        fun from(
            value: String?,
            default: ActivitySource,
        ): ActivitySource = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default
    }
}

/** The app currently being shared, already resolved against display-name overrides. */
data class ActivitySubject(
    val appLabel: String,
    val packageName: String,
    val category: String?,
)

/** The user's choices for the three text lines of the presence card. */
data class ActivityContent(
    val nameSource: ActivitySource = ActivitySource.APP,
    val detailsSource: ActivitySource = ActivitySource.APP,
    val stateSource: ActivitySource = ActivitySource.NONE,
    val nameTemplate: String = "",
    val detailsTemplate: String = "",
    val stateTemplate: String = "",
) {
    companion object {
        suspend fun load(context: Context): ActivityContent {
            val p = context.dataStore.data.first()
            return ActivityContent(
                nameSource = ActivitySource.from(p[Prefs.ActivityNameSourceKey], ActivitySource.APP),
                detailsSource = ActivitySource.from(p[Prefs.ActivityDetailsSourceKey], ActivitySource.APP),
                stateSource = ActivitySource.from(p[Prefs.ActivityStateSourceKey], ActivitySource.NONE),
                nameTemplate = p[Prefs.ActivityNameCustomKey].orEmpty(),
                detailsTemplate = p[Prefs.ActivityDetailsCustomKey].orEmpty(),
                stateTemplate = p[Prefs.ActivityStateCustomKey].orEmpty(),
            )
        }
    }
}

data class ResolvedActivityText(
    val name: String,
    val details: String?,
    val state: String?,
)

object ActivityTemplate {
    const val PLACEHOLDER_APP = "{app}"
    const val PLACEHOLDER_PACKAGE = "{package}"
    const val PLACEHOLDER_CATEGORY = "{category}"
    private const val MAX_LENGTH = 128 // Discord's limit for name / details / state
    private val whitespace = Regex("\\s{2,}")

    fun render(
        source: ActivitySource,
        template: String,
        subject: ActivitySubject,
        appName: String,
    ): String? =
        when (source) {
            ActivitySource.APP -> subject.appLabel
            ActivitySource.SHADOWRPC -> appName
            ActivitySource.PACKAGE -> subject.packageName
            ActivitySource.CATEGORY -> subject.category
            ActivitySource.CUSTOM -> expand(template, subject, appName)
            ActivitySource.NONE -> null
        }?.trim()?.take(MAX_LENGTH)?.takeIf { it.isNotEmpty() }

    fun expand(
        template: String,
        subject: ActivitySubject,
        appName: String,
    ): String =
        template
            .replace(PLACEHOLDER_APP, subject.appLabel, ignoreCase = true)
            .replace(PLACEHOLDER_PACKAGE, subject.packageName, ignoreCase = true)
            .replace(PLACEHOLDER_CATEGORY, subject.category.orEmpty(), ignoreCase = true)
            .replace("{shadowrpc}", appName, ignoreCase = true)
            .replace(whitespace, " ")
            .trim()

    /**
     * Resolves all three lines and drops repeats, the same way LunarTune's preview
     * does: with name = App and details = App the card reads just "Playing Genshin
     * Impact"; switch the name to ShadowRPC and the app moves down into details.
     */
    fun resolve(
        content: ActivityContent,
        subject: ActivitySubject,
        appName: String,
    ): ResolvedActivityText {
        val name = render(content.nameSource, content.nameTemplate, subject, appName) ?: subject.appLabel
        val details =
            render(content.detailsSource, content.detailsTemplate, subject, appName)
                ?.takeIf { it != name && it.length >= 2 }
        val state =
            render(content.stateSource, content.stateTemplate, subject, appName)
                ?.takeIf { it != name && it != details && it.length >= 2 }
        return ResolvedActivityText(name, details, state)
    }
}
