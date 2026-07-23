package com.github.helltar.anpaside.core.project

// mp3cc reports dependencies as one ^N marker per line and uses fixed text markers for failures.
internal object CompilerOutput {

    fun units(output: String) = markerValues(output, "^0")

    fun libs(output: String) = markerValues(output, "^1")

    fun stubs(output: String) = markerValues(output, "^2")

    fun hasErrors(output: String): Boolean =
        output.contains("[Pascal Error]")
                || output.contains("[Compiler Error]")
                || output.contains("Fatal error")

    fun clean(output: String): String =
        output.lineSequence()
            .filterNot { it.startsWith("@") }
            .joinToString("\n")
            .replace("[Pascal Error]", "")
            .replace("^1", "Lib: ")
            .replace("^2", "")
            .replace("^3", "")
            .trim()

    private fun markerValues(output: String, marker: String): List<String> =
        output.lineSequence()
            .filter { it.startsWith(marker) }
            .map { it.removePrefix(marker) }
            .filter { it.isNotEmpty() }
            .toList()
}
