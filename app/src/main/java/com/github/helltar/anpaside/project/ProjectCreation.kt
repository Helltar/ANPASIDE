package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.isSafeFileName

enum class CreationResult {
    CREATED,
    NAME_TOO_SHORT,
    INVALID_NAME,
    ALREADY_EXISTS,
    FAILED
}

object ProjectNames {
    const val MIN_LENGTH = 3

    fun isValidProjectName(name: String): Boolean =
        name.length >= MIN_LENGTH && name.isSafeFileName()

    fun isValidModuleName(name: String): Boolean =
        pascalIdentifier.matches(name)

    fun isValidEntryName(name: String): Boolean = name.isSafeFileName()

    fun isValidMidletName(name: String): Boolean = name.isSafeFileName()

    fun isValidMetadata(metadata: MidletMetadata): Boolean =
        isValidMidletName(metadata.name) &&
                metadata.vendor.isValidManifestValue() &&
                metadata.version.isValidManifestValue()

    fun defaultMainModuleName(projectName: String): String =
        projectName.lowercase().takeIf(::isValidModuleName) ?: DEFAULT_MAIN_MODULE

    private fun String.isValidManifestValue(): Boolean =
        isNotBlank() && trim() == this && none(Char::isISOControl)

    private val pascalIdentifier = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private const val DEFAULT_MAIN_MODULE = "main"
}

data class ProjectTemplates(
    val mainModule: String,
    val unitModule: String,
    val gitIgnore: String
)
