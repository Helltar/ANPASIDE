package com.github.helltar.anpaside.apk

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconTest {

    @Test
    fun scalesTheUsualMidletIconsByAWholeNumber() {
        // 16, 32 and 64 are what MIDletPascal projects ship; each lands just under the safe zone
        assertEquals(256, LauncherIcon.contentSize(16))
        assertEquals(264, LauncherIcon.contentSize(24))
        assertEquals(256, LauncherIcon.contentSize(32))
        assertEquals(240, LauncherIcon.contentSize(48))
        assertEquals(256, LauncherIcon.contentSize(64))
        assertEquals(256, LauncherIcon.contentSize(128))
    }

    @Test
    fun fillsTheSafeZoneWhenAWholeMultipleWouldLeaveTheIconSmall() {
        // 96 doubled covers less than three quarters of the tile, 133 barely half
        assertEquals(264, LauncherIcon.contentSize(96))
        assertEquals(264, LauncherIcon.contentSize(133))
    }

    @Test
    fun neverGrowsPastTheSafeZone() {
        assertEquals(264, LauncherIcon.contentSize(264))
        assertEquals(264, LauncherIcon.contentSize(512))
    }

    @Test
    fun survivesAnEmptyImage() {
        assertEquals(264, LauncherIcon.contentSize(0))
        assertEquals(264, LauncherIcon.contentSize(-1))
    }
}
