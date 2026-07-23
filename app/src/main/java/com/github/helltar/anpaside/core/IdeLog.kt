package com.github.helltar.anpaside.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class LogLevel { TEXT, INFO, ERROR }

data class LogMessage(val text: String, val level: LogLevel)

// the only logging channel of the app, the ui collects it into the log panel
object IdeLog {

    // replay keeps recent messages for collectors attached after emission (e.g. activity recreation)
    private val _messages = MutableSharedFlow<LogMessage>(replay = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<LogMessage> = _messages

    fun add(msg: String) = emit(msg, LogLevel.TEXT)

    fun info(msg: String) = emit(msg, LogLevel.INFO)

    fun error(msg: String) = emit(msg, LogLevel.ERROR)

    fun error(e: Exception) = error(e.message ?: e.toString())

    private fun emit(msg: String, level: LogLevel) {
        val text = msg.trim()

        if (text.isNotEmpty()) {
            _messages.tryEmit(LogMessage(text, level))
        }
    }
}
