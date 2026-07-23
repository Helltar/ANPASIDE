package com.github.helltar.anpaside.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// fallback brand palette, used when dynamic color is not available (android < 12)
val Blue80 = Color(0xFF9FCAFF)
val BlueGrey80 = Color(0xFFBFC7DC)
val Cyan80 = Color(0xFF8ED9E8)

val Blue40 = Color(0xFF097DE3)
val BlueGrey40 = Color(0xFF555F71)
val Cyan40 = Color(0xFF00697C)

// syntax highlighting is not part of the material scheme, it is picked per light/dark
data class SyntaxColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val lineNumber: Color,
    val logInfo: Color
)

val DarkSyntaxColors = SyntaxColors(
    keyword = Color(0xFF00BEE6),
    string = Color(0xFFFFCC33),
    number = Color(0xFFFF6633),
    comment = Color(0xFF0AC80A),
    lineNumber = Color(0xFF6E6E6E),
    logInfo = Color(0xFF35C135)
)

val LightSyntaxColors = SyntaxColors(
    keyword = Color(0xFF0057B7),
    string = Color(0xFF9A6700),
    number = Color(0xFFC0392B),
    comment = Color(0xFF2E7D32),
    lineNumber = Color(0xFF9E9E9E),
    logInfo = Color(0xFF1B7A1B)
)

val LocalSyntaxColors = staticCompositionLocalOf { DarkSyntaxColors }
