package com.github.helltar.anpaside.foundation

sealed interface ProcessResult {
    data class Completed(val exitCode: Int, val output: String) : ProcessResult
    data class Failed(val error: Exception) : ProcessResult
}

fun interface ProcessRunner {
    fun run(arguments: List<String>): ProcessResult
}

class SystemProcessRunner : ProcessRunner {

    override fun run(arguments: List<String>): ProcessResult =
        try {
            val process = ProcessBuilder(arguments).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            ProcessResult.Completed(process.waitFor(), output)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            ProcessResult.Failed(error)
        } catch (error: Exception) {
            ProcessResult.Failed(error)
        }
}
