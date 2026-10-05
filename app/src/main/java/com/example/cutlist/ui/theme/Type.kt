package com.example.cutlist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val Typography = base.copy(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
)

/** Tabular figures keep dimension columns aligned. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Small uppercase-friendly overline used for section labels. */
val TextStyle.overline: TextStyle
    get() = copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
