package dev.citali.shadowrpc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.citali.shadowrpc.R

/** Bundled Montserrat (SIL OFL 1.1); static instances also support Android 8. */
val Montserrat = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
)

val ShadowTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Montserrat),
        displayMedium = base.displayMedium.copy(fontFamily = Montserrat),
        displaySmall = base.displaySmall.copy(fontFamily = Montserrat, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
        headlineLarge = base.headlineLarge.copy(fontFamily = Montserrat),
        headlineMedium = base.headlineMedium.copy(fontFamily = Montserrat, fontWeight = FontWeight.Normal),
        headlineSmall = base.headlineSmall.copy(fontFamily = Montserrat),
        titleLarge = base.titleLarge.copy(fontFamily = Montserrat, fontWeight = FontWeight.Medium),
        titleMedium = base.titleMedium.copy(fontFamily = Montserrat),
        titleSmall = base.titleSmall.copy(fontFamily = Montserrat),
        bodyLarge = base.bodyLarge.copy(fontFamily = Montserrat),
        bodyMedium = base.bodyMedium.copy(fontFamily = Montserrat),
        bodySmall = base.bodySmall.copy(fontFamily = Montserrat),
        labelLarge = base.labelLarge.copy(fontFamily = Montserrat),
        labelMedium = base.labelMedium.copy(fontFamily = Montserrat),
        labelSmall = base.labelSmall.copy(fontFamily = Montserrat),
    )
}
