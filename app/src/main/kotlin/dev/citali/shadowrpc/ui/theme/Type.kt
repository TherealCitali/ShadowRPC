package dev.citali.shadowrpc.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ShadowTypography =
    Typography().let { base ->
        base.copy(
            displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
