package com.github.helltar.anpaside.compiler

// mp3cc reports dependencies as one ^N marker per line and uses fixed text markers for failures.
internal object CompilerOutput {

    fun units(output: String) = markerValues(output, "^0")

    fun libraries(output: String) = markerValues(output, "^1")

    fun runtimeClasses(output: String) = markerValues(output, "^2")

    fun recordClasses(output: String) = markerValues(output, "^3")

    // record classes are numbered per compiler run, not per project: without -r every module
    // starts at R_0 again and overwrites the record classes of the ones compiled before it
    fun nextRecordId(output: String, current: Int): Int =
        recordClasses(output)
            .mapNotNull { RECORD_CLASS.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?.let { maxOf(current, it + 1) }
            ?: current

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

    private val RECORD_CLASS = Regex("""R_(\d+)\.class""")
}
