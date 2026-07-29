package com.github.helltar.anpaside.apk

import java.io.File
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Writes a new apk out of the template, entry by entry.
 *
 * Replacing a file inside an archive means rewriting it anyway - every later entry moves - so
 * the template is streamed through instead of being copied and edited in place. Alignment is
 * not this class's business: the signer rewrites the archive once more and aligns every stored
 * entry itself, which is why `resources.arsc`, the one file Android insists on reading without
 * inflating it, only has to be left uncompressed here.
 */
object ApkArchive {

    private val signatureFiles = Regex("META-INF/[^/]+\\.(SF|RSA|DSA|EC)|META-INF/MANIFEST\\.MF")

    fun repack(
        template: InputStream,
        target: File,
        replacements: Map<String, ByteArray>,
        additions: Map<String, File>
    ) {
        ZipOutputStream(target.outputStream().buffered()).use { output ->
            ZipInputStream(template).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val name = entry.name

                    if (entry.isDirectory || name in additions || signatureFiles.matches(name)) {
                        continue
                    }

                    write(output, name, replacements[name] ?: input.readBytes())
                }
            }

            for ((name, file) in additions) {
                write(output, name, file.readBytes())
            }
        }
    }

    private fun write(output: ZipOutputStream, name: String, content: ByteArray) {
        val entry = ZipEntry(name)

        if (name == ApkTemplate.RESOURCE_TABLE_ENTRY) {
            entry.method = ZipEntry.STORED
            entry.size = content.size.toLong()
            entry.compressedSize = content.size.toLong()
            entry.crc = CRC32().apply { update(content) }.value
        } else {
            entry.method = ZipEntry.DEFLATED
        }

        output.putNextEntry(entry)
        output.write(content)
        output.closeEntry()
    }
}
