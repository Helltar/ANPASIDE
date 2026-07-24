package com.github.helltar.anpaside.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectNamesTest {

    @Test
    fun projectNamesAllowSpacesButNeverPaths() {
        assertTrue(ProjectNames.isValidProjectName("My Game"))
        assertFalse(ProjectNames.isValidProjectName("ab"))
        assertFalse(ProjectNames.isValidProjectName("../game"))
        assertFalse(ProjectNames.isValidProjectName("games/demo"))
    }

    @Test
    fun moduleNamesFollowPascalIdentifierRules() {
        assertTrue(ProjectNames.isValidModuleName("game_2"))
        assertTrue(ProjectNames.isValidModuleName("_ui"))
        assertFalse(ProjectNames.isValidModuleName("2game"))
        assertFalse(ProjectNames.isValidModuleName("game unit"))
        assertFalse(ProjectNames.isValidModuleName("../unit"))
    }

    @Test
    fun projectNamesProduceAValidDefaultModuleName() {
        assertEquals("demo", ProjectNames.defaultMainModuleName("Demo"))
        assertEquals("main", ProjectNames.defaultMainModuleName("My Game"))
        assertEquals("main", ProjectNames.defaultMainModuleName("Гра"))
    }

    @Test
    fun metadataMustBeSafeForBothManifestAndJar() {
        assertTrue(ProjectNames.isValidMetadata(MidletMetadata("My Game", "Vendor", "1.0")))
        assertFalse(ProjectNames.isValidMetadata(MidletMetadata("My/Game", "Vendor", "1.0")))
        assertFalse(ProjectNames.isValidMetadata(MidletMetadata("Game", "Vendor\nInjected", "1.0")))
        assertFalse(ProjectNames.isValidMetadata(MidletMetadata("Game", "Vendor", "")))
    }
}
