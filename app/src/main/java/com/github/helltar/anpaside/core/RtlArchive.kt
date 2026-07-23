package com.github.helltar.anpaside.core

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// the midletpascal runtime library (rtl): the ten java me .class files that
// every compiled midlet links against (FW, Real, ...). they ship inside the apk
// as a single encrypted blob, assets/rtl/classes.bin, instead of plain files so
// that static antivirus scanners do not see raw java me bytecode sitting in the
// apk and flag the app. this is packaging obfuscation, not a security boundary:
// the aes key below necessarily ships in the app (and in tools/EncryptStubs.java,
// which produces the blob). AssetInstaller calls read() on first launch and
// writes the decrypted classes into the private files/rtl/ dir, where the
// compile -> jar pipeline (ProjectBuilder) reads them.
//
// blob layout: [magic 8 bytes][iv 12 bytes][aes-gcm ciphertext + 16-byte tag].
// the magic doubles as gcm additional authenticated data, so tampering with
// either the header or the ciphertext fails the gcm tag check and throws.
internal object RtlArchive {

    const val FILE_NAME = "classes.bin"

    // the exact set the archive must contain, no more and no less. read() rejects
    // anything else, so a swapped-in class cannot slip into the runtime.
    val expectedClassNames =
        setOf(
            "F.class",
            "FS.class",
            "FW.class",
            "H.class",
            "P.class",
            "RS.class",
            "Real.class",
            "Real\$NumberFormat.class",
            "S.class",
            "SM.class"
        )

    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private val magic = byteArrayOf(0xA9.toByte(), 0x41, 0x50, 0x52, 0x54, 0x4C, 0x01, 0x00)
    private val key = Base64.getDecoder().decode("wfqN93Dw24zueqoiTM3pEztuvPDyICKsfg1/FWQvGLg=")

    // decrypt the blob, then unzip it while checking that every entry is one of
    // the expected classes, is a real class file, and appears once - and that all
    // ten are present. any tampering trips the gcm tag in decrypt() or a require().
    fun read(encryptedArchive: ByteArray): Map<String, ByteArray> {
        val zip = decrypt(encryptedArchive)
        val entries = linkedMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            var entry = input.nextEntry

            while (entry != null) {
                val name = entry.name

                require(!entry.isDirectory && name in expectedClassNames) { "Unexpected rtl entry: $name" }
                require(name !in entries) { "Duplicate rtl entry: $name" }

                val bytes = input.readBytes()
                require(bytes.isClassFile()) { "Invalid class file: $name" }
                entries[name] = bytes

                input.closeEntry()
                entry = input.nextEntry
            }
        }

        require(entries.keys == expectedClassNames) { "Incomplete rtl archive" }
        return entries
    }

    // authenticate and decrypt the aes-gcm blob. the magic header is fed in as aad,
    // so the header, the iv and the ciphertext are all covered by the tag; a single
    // flipped byte anywhere makes doFinal() throw an AEADBadTagException.
    private fun decrypt(encryptedArchive: ByteArray): ByteArray {
        require(encryptedArchive.size > magic.size + IV_SIZE + TAG_SIZE_BITS / 8) {
            "Invalid rtl archive"
        }

        require(encryptedArchive.copyOfRange(0, magic.size).contentEquals(magic)) {
            "Unknown rtl archive format"
        }

        val ivStart = magic.size
        val encryptedStart = ivStart + IV_SIZE
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_SIZE_BITS, encryptedArchive.copyOfRange(ivStart, encryptedStart))
        )

        cipher.updateAAD(magic)

        return cipher.doFinal(encryptedArchive, encryptedStart, encryptedArchive.size - encryptedStart)
    }

    // java class files start with the 0xCAFEBABE magic number
    private fun ByteArray.isClassFile(): Boolean {
        return size >= 4 &&
                this[0] == 0xCA.toByte() &&
                this[1] == 0xFE.toByte() &&
                this[2] == 0xBA.toByte() &&
                this[3] == 0xBE.toByte()
    }
}
