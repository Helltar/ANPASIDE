package com.github.helltar.anpaside.apk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The little that is needed to edit a compiled AndroidManifest.xml.
 *
 * An apk carries its manifest in Android's binary resource format, and there is no aapt2 on a
 * phone to produce one. Everything an export has to change - the package name, the label, the
 * version - is either a string in the manifest's string pool or a single integer attribute, so
 * the file is rewritten instead of generated: every chunk that is not touched is copied through
 * byte for byte.
 *
 * Layout of the format, all little endian:
 * ```
 * ResChunk_header       u16 type, u16 headerSize, u32 size
 * ResStringPool_header  header, u32 stringCount, u32 styleCount, u32 flags,
 *                       u32 stringsStart, u32 stylesStart
 * ResXMLTree_node       header, u32 lineNumber, u32 comment
 * ResXMLTree_attrExt    u32 namespace, u32 name, u16 attributeStart, u16 attributeSize,
 *                       u16 attributeCount, u16 idIndex, u16 classIndex, u16 styleIndex
 * ResXMLTree_attribute  u32 namespace, u32 name, u32 rawValue,
 *                       u16 size, u8 res0, u8 dataType, u32 data
 * ```
 */
object BinaryXml {

    private const val CHUNK_XML = 0x0003
    private const val CHUNK_STRING_POOL = 0x0001
    private const val CHUNK_START_ELEMENT = 0x0102

    private const val CHUNK_HEADER_SIZE = 8
    private const val STRING_POOL_HEADER_SIZE = 28
    private const val NODE_HEADER_SIZE = 16
    private const val ATTRIBUTE_SIZE = 20

    private const val FLAG_UTF8 = 1 shl 8

    private const val TYPE_INT_DEC = 0x10
    private const val NO_ENTRY = -1

    /**
     * Returns a copy of [source] in which every string of the pool is replaced by [transform].
     *
     * Entries keep their order and their count, because attribute values and the resource map
     * chunk that follows the pool address them by index.
     */
    fun rewriteStrings(source: ByteArray, transform: (String) -> String): ByteArray {
        val poolOffset = stringPoolOffset(source)
        val pool = StringPool.read(source, poolOffset)
        val encoded = pool.write(pool.strings.map(transform))
        val tail = poolOffset + source.int32(poolOffset + 4)

        val output = ByteArrayOutputStream(source.size)
        output.write(source, 0, poolOffset)
        output.write(encoded)
        output.write(source, tail, source.size - tail)

        return output.toByteArray().also { patched -> patched.putInt32(4, patched.size) }
    }

    /**
     * Sets an integer attribute of the root element, the one manifest attribute an export has
     * to change that does not live in the string pool. Returns [source] unchanged when the
     * attribute is absent.
     */
    fun setRootIntAttribute(source: ByteArray, name: String, value: Int): ByteArray {
        val patched = source.copyOf()
        val nameIndex = StringPool.read(patched, stringPoolOffset(patched)).strings.indexOf(name)
        val element = firstStartElement(patched)

        if (nameIndex < 0 || element == null) {
            return source
        }

        val attributeStart = patched.uint16(element + NODE_HEADER_SIZE + 8)
        val attributeCount = patched.uint16(element + NODE_HEADER_SIZE + 12)
        var attribute = element + NODE_HEADER_SIZE + attributeStart

        repeat(attributeCount) {
            if (patched.int32(attribute + 4) == nameIndex) {
                // a typed value replaces whatever raw string the attribute carried
                patched.putInt32(attribute + 8, NO_ENTRY)
                patched[attribute + 15] = TYPE_INT_DEC.toByte()
                patched.putInt32(attribute + 16, value)
                return patched
            }

            attribute += ATTRIBUTE_SIZE
        }

        return source
    }

    private fun stringPoolOffset(source: ByteArray): Int {
        require(source.size > CHUNK_HEADER_SIZE && source.uint16(0) == CHUNK_XML) {
            "Not an Android binary XML file"
        }

        val offset = source.uint16(2)

        require(offset + CHUNK_HEADER_SIZE <= source.size && source.uint16(offset) == CHUNK_STRING_POOL) {
            "Binary XML without a string pool chunk"
        }

        return offset
    }

    private fun firstStartElement(source: ByteArray): Int? {
        var offset = source.uint16(2)

        while (offset + CHUNK_HEADER_SIZE <= source.size) {
            if (source.uint16(offset) == CHUNK_START_ELEMENT) {
                return offset
            }

            val size = source.int32(offset + 4)

            if (size <= 0) {
                return null
            }

            offset += size
        }

        return null
    }

    /**
     * A parsed string pool chunk: the strings themselves plus what is needed to write the chunk
     * back in the encoding it arrived in. Style data is opaque here - it addresses strings by
     * index rather than by offset, so it survives a rewrite untouched.
     */
    private class StringPool(
        val strings: List<String>,
        private val flags: Int,
        private val styleOffsets: IntArray,
        private val styleData: ByteArray
    ) {

        private val isUtf8: Boolean
            get() = flags and FLAG_UTF8 != 0

        fun write(replacements: List<String>): ByteArray {
            require(replacements.size == strings.size) { "String count must not change" }

            val offsets = IntArray(replacements.size)
            val data = ByteArrayOutputStream()

            for ((index, string) in replacements.withIndex()) {
                offsets[index] = data.size()
                data.write(if (isUtf8) encodeUtf8(string) else encodeUtf16(string))
            }

            while (data.size() % 4 != 0) {
                data.write(0)
            }

            val stringsStart = STRING_POOL_HEADER_SIZE + (offsets.size + styleOffsets.size) * 4
            val stylesStart = if (styleOffsets.isEmpty()) 0 else stringsStart + data.size()
            val size = stringsStart + data.size() + styleData.size

            val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putShort(CHUNK_STRING_POOL.toShort())
            buffer.putShort(STRING_POOL_HEADER_SIZE.toShort())
            buffer.putInt(size)
            buffer.putInt(offsets.size)
            buffer.putInt(styleOffsets.size)
            buffer.putInt(flags)
            buffer.putInt(stringsStart)
            buffer.putInt(stylesStart)
            offsets.forEach { offset -> buffer.putInt(offset) }
            styleOffsets.forEach { offset -> buffer.putInt(offset) }
            buffer.put(data.toByteArray())
            buffer.put(styleData)

            return buffer.array()
        }

        // both encodings prefix the length, and both spell a length above their single unit
        // limit over two units, with the high bit of the first one set
        private fun encodeUtf8(string: String): ByteArray {
            val bytes = string.toByteArray(Charsets.UTF_8)
            val output = ByteArrayOutputStream(bytes.size + 5)

            writeUtf8Length(output, string.length)
            writeUtf8Length(output, bytes.size)
            output.write(bytes)
            output.write(0)

            return output.toByteArray()
        }

        private fun writeUtf8Length(output: ByteArrayOutputStream, length: Int) {
            if (length > 0x7F) {
                output.write(((length shr 8) or 0x80) and 0xFF)
            }

            output.write(length and 0xFF)
        }

        private fun encodeUtf16(string: String): ByteArray {
            val chars = string.toByteArray(Charsets.UTF_16LE)
            val units = chars.size / 2
            val prefix = if (units > 0x7FFF) 4 else 2
            val buffer =
                ByteBuffer.allocate(prefix + chars.size + 2).order(ByteOrder.LITTLE_ENDIAN)

            if (prefix == 4) {
                buffer.putShort(((units shr 16) or 0x8000).toShort())
            }

            buffer.putShort((units and 0xFFFF).toShort())
            buffer.put(chars)
            buffer.putShort(0)

            return buffer.array()
        }

        companion object {

            fun read(source: ByteArray, offset: Int): StringPool {
                val size = source.int32(offset + 4)
                val stringCount = source.int32(offset + 8)
                val styleCount = source.int32(offset + 12)
                val flags = source.int32(offset + 16)
                val stringsStart = source.int32(offset + 20)
                val stylesStart = source.int32(offset + 24)
                val isUtf8 = flags and FLAG_UTF8 != 0

                val strings =
                    (0 until stringCount).map { index ->
                        val start =
                            offset +
                                    stringsStart +
                                    source.int32(offset + STRING_POOL_HEADER_SIZE + index * 4)

                        if (isUtf8) decodeUtf8(source, start) else decodeUtf16(source, start)
                    }

                val styleOffsets =
                    IntArray(styleCount) { index ->
                        source.int32(offset + STRING_POOL_HEADER_SIZE + (stringCount + index) * 4)
                    }

                val styleData =
                    if (styleCount == 0) {
                        ByteArray(0)
                    } else {
                        source.copyOfRange(offset + stylesStart, offset + size)
                    }

                return StringPool(strings, flags, styleOffsets, styleData)
            }

            private fun decodeUtf8(source: ByteArray, offset: Int): String {
                var position = offset + utf8LengthSize(source, offset)
                val byteCount = utf8Length(source, position)
                position += utf8LengthSize(source, position)

                return String(source, position, byteCount, Charsets.UTF_8)
            }

            private fun utf8Length(source: ByteArray, offset: Int): Int {
                val first = source[offset].toInt() and 0xFF

                return if (first and 0x80 == 0) {
                    first
                } else {
                    ((first and 0x7F) shl 8) or (source[offset + 1].toInt() and 0xFF)
                }
            }

            private fun utf8LengthSize(source: ByteArray, offset: Int): Int =
                if (source[offset].toInt() and 0x80 == 0) 1 else 2

            private fun decodeUtf16(source: ByteArray, offset: Int): String {
                var position = offset
                var units = source.uint16(position)
                position += 2

                if (units and 0x8000 != 0) {
                    units = ((units and 0x7FFF) shl 16) or source.uint16(position)
                    position += 2
                }

                return String(source, position, units * 2, Charsets.UTF_16LE)
            }
        }
    }
}

private fun ByteArray.uint16(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.int32(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.putInt32(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value shr 8).toByte()
    this[offset + 2] = (value shr 16).toByte()
    this[offset + 3] = (value shr 24).toByte()
}
