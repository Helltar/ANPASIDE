package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HexColorTest {

    @Test
    fun parsesSixDigitColoursAsOpaque() {
        assertEquals(0xFF3F444C.toInt(), HexColor.parse("#3F444C"))
        assertEquals(0xFFFFFFFF.toInt(), HexColor.parse("#ffffff"))
        assertEquals(0xFF000000.toInt(), HexColor.parse("#000000"))
    }

    @Test
    fun rejectsEverythingAHandCanTypeWrong() {
        // an .aproj is edited by hand as often as through the dialog
        for (value in listOf("3F444C", "#3F444", "#3F444CC", "#GGGGGG", "", "#", "red")) {
            assertFalse(value, HexColor.isValid(value))
            assertNull(value, HexColor.parse(value))
        }
    }

    @Test
    fun roundTripsThroughText() {
        assertEquals("#3F444C", HexColor.format(HexColor.parse("#3F444C")!!))
        assertEquals("#00FF7F", HexColor.format(HexColor.parse("#00ff7f")!!))
    }

    @Test
    fun apkSettingsAreValidatedAsAWhole() {
        val valid =
            ApkSettings(
                packageName = "midlet.tank",
                label = "Tank",
                versionCode = 7,
                iconBackground = ApkSettings.DEFAULT_ICON_BACKGROUND,
                orientation = ApkOrientation.PORTRAIT,
                keyboardEnabled = false
            )

        assertTrue(ProjectNames.isValidApkSettings(valid))
        // an empty label and an absent version code are the "follow the MIDlet metadata" cases
        assertTrue(ProjectNames.isValidApkSettings(valid.copy(label = "", versionCode = null)))

        assertFalse(ProjectNames.isValidApkSettings(valid.copy(packageName = "tank")))
        assertFalse(ProjectNames.isValidApkSettings(valid.copy(label = " Tank")))
        assertFalse(ProjectNames.isValidApkSettings(valid.copy(label = "Tank\nHD")))
        assertFalse(ProjectNames.isValidApkSettings(valid.copy(versionCode = 0)))
        assertFalse(ProjectNames.isValidApkSettings(valid.copy(iconBackground = "3F444C")))
    }
}
