package com.github.helltar.anpaside.foundation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class LogSeverity { PLAIN, INFO, ERROR }

data class LogEntry(val text: String, val severity: LogSeverity)

class IdeLogger {

    // replay keeps startup messages available across activity recreation
    private val mutableEntries =
        MutableSharedFlow<LogEntry>(
            replay = REPLAY_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    val entries: SharedFlow<LogEntry> = mutableEntries.asSharedFlow()

    fun plain(message: String) = emit(message, LogSeverity.PLAIN)

    fun info(message: String) = emit(message, LogSeverity.INFO)

    fun error(message: String) = emit(message, LogSeverity.ERROR)

    fun error(error: Throwable) = error(error.message ?: error.toString())

    private fun emit(message: String, severity: LogSeverity) {
        val text = message.trim()

        if (text.isNotEmpty()) {
            mutableEntries.tryEmit(LogEntry(text, severity))
        }
    }

    private companion object {
        const val REPLAY_CAPACITY = 64
    }
}

