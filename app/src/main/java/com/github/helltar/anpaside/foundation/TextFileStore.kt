package com.github.helltar.anpaside.foundation

import java.io.File

class TextFileStore {

    fun read(file: File): String = file.readText()

    fun write(file: File, text: String) = file.writeTextAtomically(text)
}
