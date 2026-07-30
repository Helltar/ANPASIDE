package com.github.helltar.anpaside.project

/**
 * The `#RRGGBB` colours a project config stores.
 *
 * Written by hand into an `.aproj` as often as it is picked in the dialog, so anything the parser
 * does not recognise has to fall back rather than throw. There is no alpha: the layers of a
 * launcher icon are always opaque, and a half transparent tile under a sprite only looks broken.
 */
object HexColor {

    private const val OPAQUE = 0xFF000000.toInt()
    private val pattern = Regex("#[0-9a-fA-F]{6}")

    fun isValid(value: String): Boolean = pattern.matches(value)

    fun parse(value: String): Int? =
        value.takeIf(::isValid)?.let { hex -> OPAQUE or hex.substring(1).toInt(16) }

    fun format(color: Int): String = "#%06X".format(color and 0xFFFFFF)
}
