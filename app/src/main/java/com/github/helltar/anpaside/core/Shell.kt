package com.github.helltar.anpaside.core

// exitCode is meaningful only when started is true; mp3cc returns its error count,
// so anything but 0 means the compilation did not finish
data class ShellResult(val started: Boolean, val exitCode: Int, val output: String)

fun runCommand(args: List<String>): ShellResult {
    return try {
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        ShellResult(true, process.waitFor(), output)
    } catch (e: Exception) {
        IdeLog.error(e)
        ShellResult(false, -1, "")
    }
}
