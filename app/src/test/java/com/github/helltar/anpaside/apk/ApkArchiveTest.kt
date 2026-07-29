package com.github.helltar.anpaside.apk

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkArchiveTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun replacesAddsAndDropsEntries() {
        val target = temporaryFolder.newFile("out.apk")
        val dex = temporaryFolder.newFile("converted.dex").apply { writeText("dex") }

        ApkArchive.repack(
            template = ByteArrayInputStream(template()),
            target = target,
            replacements = mapOf(ApkTemplate.MANIFEST_ENTRY to "patched".toByteArray()),
            additions = mapOf("assets/midlet/converted.dex" to dex)
        )

        ZipFile(target).use { apk ->
            assertEquals("patched", apk.read(ApkTemplate.MANIFEST_ENTRY))
            assertEquals("dex", apk.read("assets/midlet/converted.dex"))
            assertEquals("classes", apk.read("classes.dex"))

            // a signature of the template would survive into the export and invalidate it
            assertFalse(apk.entries().toList().any { entry -> entry.name.startsWith("META-INF/") })
        }
    }

    @Test
    fun leavesTheResourceTableUncompressedForTheSignerToAlign() {
        val target = temporaryFolder.newFile("out.apk")

        ApkArchive.repack(ByteArrayInputStream(template()), target, emptyMap(), emptyMap())

        ZipFile(target).use { apk ->
            assertEquals(
                ZipEntry.STORED,
                apk.getEntry(ApkTemplate.RESOURCE_TABLE_ENTRY).method
            )

            assertEquals(ZipEntry.DEFLATED, apk.getEntry("classes.dex").method)
        }
    }

    @Test
    fun keepsAnEntryThatIsBothReplacedAndAdded() {
        val target = temporaryFolder.newFile("out.apk")
        val added = temporaryFolder.newFile("added").apply { writeText("added") }

        ApkArchive.repack(
            template = ByteArrayInputStream(template()),
            target = target,
            replacements = emptyMap(),
            additions = mapOf("classes.dex" to added)
        )

        ZipFile(target).use { apk ->
            assertEquals("added", apk.read("classes.dex"))
            assertEquals(1, apk.entries().toList().count { entry -> entry.name == "classes.dex" })
        }
    }

    private fun template(): ByteArray {
        val output = ByteArrayOutputStream()

        ZipOutputStream(output).use { zip ->
            zip.write(ApkTemplate.MANIFEST_ENTRY, "manifest")
            zip.write("classes.dex", "classes")
            zip.write(ApkTemplate.RESOURCE_TABLE_ENTRY, "resources")
            zip.write("res/mipmap-mdpi-v4/midlet_icon.png", "icon")
            zip.write("META-INF/CERT.SF", "signature")
            zip.write("META-INF/MANIFEST.MF", "signature")
        }

        return output.toByteArray()
    }

    private fun ZipOutputStream.write(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }

    private fun ZipFile.read(name: String): String =
        getInputStream(getEntry(name)).use { input -> input.readBytes().decodeToString() }
}
