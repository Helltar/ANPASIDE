package com.github.helltar.anpaside.compiler

data class BuildMessages(
    val manifestTemplate: String,
    val fileNotFound: String,
    val compilerExitTemplate: String,
    val mainClassMissing: String,
    val archiveCreationFailed: String,
    val buildSucceeded: String
) {
    fun compilerExit(exitCode: Int): String = compilerExitTemplate.format(exitCode)
}

