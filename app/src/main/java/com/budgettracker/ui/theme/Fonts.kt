package com.budgettracker.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.budgettracker.R

/**
 * Numeric/tabular font for amounts and figures.
 * JetBrains Mono (SIL OFL 1.1) — even-width digits keep figures aligned
 * and crisp at small sizes. Weights used across the UI: Light, Regular,
 * Medium, SemiBold, Bold and ExtraBold (Black maps to ExtraBold).
 */
val NumeralFont = FontFamily(
    Font(R.font.jetbrains_mono_light, FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold)
)
