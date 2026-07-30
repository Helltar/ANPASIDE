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

    fun isValidPackageName(name: String): Boolean = packageName.matches(name)

    // an empty label is the "use the MIDlet name" case, so only a label that is actually there
    // has to survive going into a manifest
    fun isValidAppLabel(label: String): Boolean = label.isEmpty() || label.isValidManifestValue()

    fun isValidApkSettings(settings: ApkSettings): Boolean =
        isValidPackageName(settings.packageName) &&
                isValidAppLabel(settings.label) &&
                HexColor.isValid(settings.iconBackground) &&
                (settings.versionCode == null || settings.versionCode >= Project.MIN_VERSION_CODE)

    // android needs at least two segments, each starting with a letter, and nothing but the
    // midlet name is known here to build one from
    fun defaultPackageName(midletName: String): String {
        val segment =
            midletName
                .lowercase()
                .replace(invalidPackageCharacters, "_")
                .dropWhile { character -> !character.isLetter() }
                .ifEmpty { DEFAULT_PACKAGE_SEGMENT }

        return "$PACKAGE_ROOT.$segment"
    }

    private fun String.isValidManifestValue(): Boolean =
        isNotBlank() && trim() == this && none(Char::isISOControl)

    private val pascalIdentifier = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val packageName = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    private val invalidPackageCharacters = Regex("[^a-z0-9_]")
    private const val DEFAULT_MAIN_MODULE = "main"
    private const val PACKAGE_ROOT = "midlet"
    private const val DEFAULT_PACKAGE_SEGMENT = "app"
}

data class ProjectTemplates(
    val mainModule: String,
    val unitModule: String,
    val gitIgnore: String
)
