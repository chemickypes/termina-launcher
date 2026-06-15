package com.example.terminalauncher.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val TermTypography =
  Typography(
    bodyLarge =
      TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 19.sp),
    bodyMedium =
      TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp),
    titleMedium =
      TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 20.sp),
  )
