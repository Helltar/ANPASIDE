package com.github.helltar.anpaside.apk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BinaryXmlTest {

    @Test
    fun replacesStringsAndLeavesEveryOtherChunkAlone() {
        val source = manifest(utf8 = false)
        val patched =
            BinaryXml.rewriteStrings(source) { value ->
                if (value == "com.example.app") "midlet.tank" else value
            }

        assertEquals(
            listOf("versionCode", "package", "midlet.tank", "1.0"),
            AxmlReader(patched).strings
        )

        assertArrayEquals(AxmlReader(source).tail, AxmlReader(patched).tail)
    }

    @Test
    fun readsAndWritesUtf8Pools() {
        val patched =
            BinaryXml.rewriteStrings(manifest(utf8 = true)) { value ->
                if (value == "com.example.app") "midlet.ця_гра" else value
            }

        assertEquals("midlet.ця_гра", AxmlReader(patched).strings[2])
    }

    @Test
    fun keepsTheDeclaredSizeOfTheFileInSyncWithItsContent() {
        val patched = BinaryXml.rewriteStrings(manifest(utf8 = false)) { "$it and more" }

        assertEquals(patched.size, AxmlReader(patched).declaredSize)
    }

    @Test
    fun setsTheVersionCodeOfTheRootElement() {
        val patched = BinaryXml.setRootIntAttribute(manifest(utf8 = false), "versionCode", 10203)

        assertEquals(10203, AxmlReader(patched).rootIntAttribute(index = 1))
    }

    @Test
    fun leavesTheFileAloneWhenTheAttributeIsMissing() {
        val source = manifest(utf8 = false)

        assertArrayEquals(source, BinaryXml.setRootIntAttribute(source, "versionMajor", 7))
    }

    /**
     * A manifest with the two attributes an export changes: `package`, a string, and
     * `versionCode`, an integer.
     */
    private fun manifest(utf8: Boolean): ByteArray {
        val strings = listOf("versionCode", "package", "com.example.app", "1.0")
        val pool = stringPool(strings, utf8)

        val element = ByteBuffer.allocate(16 + 20 + 2 * 20).order(ByteOrder.LITTLE_ENDIAN)
        element.putShort(0x0102)
        element.putShort(16)
        element.putInt(element.capacity())
        element.putInt(1)
        element.putInt(-1)
        element.putInt(-1)
        element.putInt(-1)
        element.putShort(20)
        element.putShort(20)
        element.putShort(2)
        element.putShort(0)
        element.putShort(0)
        element.putShort(0)
        attribute(element, name = 1, rawValue = 2, dataType = 0x03, data = 2)
        attribute(element, name = 0, rawValue = 3, dataType = 0x10, data = 1)

        val body = pool + element.array()
        val output = ByteBuffer.allocate(8 + body.size).order(ByteOrder.LITTLE_ENDIAN)
        output.putShort(0x0003)
        output.putShort(8)
        output.putInt(8 + body.size)
        output.put(body)

        return output.array()
    }

    private fun attribute(
        buffer: ByteBuffer,
        name: Int,
        rawValue: Int,
        dataType: Int,
        data: Int
    ) {
        buffer.putInt(-1)
        buffer.putInt(name)
        buffer.putInt(rawValue)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(dataType.toByte())
        buffer.putInt(data)
    }

    private fun stringPool(strings: List<String>, utf8: Boolean): ByteArray {
        val data = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()

        for (string in strings) {
            offsets += data.size()

            if (utf8) {
                val bytes = string.toByteArray(Charsets.UTF_8)
                data.write(string.length)
                data.write(bytes.size)
                data.write(bytes)
                data.write(0)
            } else {
                val bytes = string.toByteArray(Charsets.UTF_16LE)
                data.write(string.length and 0xFF)
                data.write(string.length shr 8)
                data.write(bytes)
                data.write(0)
                data.write(0)
            }
        }

        while (data.size() % 4 != 0) {
            data.write(0)
        }

        val stringsStart = 28 + offsets.size * 4
        val size = stringsStart + data.size()
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(0x0001)
        buffer.putShort(28)
        buffer.putInt(size)
        buffer.putInt(offsets.size)
        buffer.putInt(0)
        buffer.putInt(if (utf8) 1 shl 8 else 0)
        buffer.putInt(stringsStart)
        buffer.putInt(0)
        offsets.forEach { offset -> buffer.putInt(offset) }
        buffer.put(data.toByteArray())

        return buffer.array()
    }

    /** Reads back only what the tests assert on. */
    private class AxmlReader(private val source: ByteArray) {

        private val buffer = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN)
        private val poolOffset = buffer.getShort(2).toInt()
        private val poolSize = buffer.getInt(poolOffset + 4)

        val declaredSize: Int get() = buffer.getInt(4)

        val tail: ByteArray get() = source.copyOfRange(poolOffset + poolSize, source.size)

        val strings: List<String>
            get() {
                val count = buffer.getInt(poolOffset + 8)
                val utf8 = buffer.getInt(poolOffset + 16) and (1 shl 8) != 0
                val start = poolOffset + buffer.getInt(poolOffset + 20)

                return (0 until count).map { index ->
                    val at = start + buffer.getInt(poolOffset + 28 + index * 4)

                    if (utf8) {
                        val length = source[at + 1].toInt() and 0xFF
                        String(source, at + 2, length, Charsets.UTF_8)
                    } else {
                        val length = buffer.getShort(at).toInt() and 0xFFFF
                        String(source, at + 2, length * 2, Charsets.UTF_16LE)
                    }
                }
            }

        fun rootIntAttribute(index: Int): Int {
            val element = poolOffset + poolSize
            return buffer.getInt(element + 16 + 20 + index * 20 + 16)
        }
    }
}
