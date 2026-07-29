package com.github.helltar.anpaside.project

import com.github.helltar.anpaside.foundation.ProjectLayout
import com.github.helltar.anpaside.foundation.createDirectories
import com.github.helltar.anpaside.foundation.requireDirectChildName
import com.github.helltar.anpaside.foundation.writeAtomically
import java.io.File
import java.util.Properties

data class MidletMetadata(
    val name: String,
    val vendor: String,
    val version: String
)

data class CompilerSettings(
    val mathType: Int,
    val canvasType: Int
)

enum class ApkOrientation(val propertyValue: String) {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    companion object {
        fun fromProperty(value: String?): ApkOrientation =
            entries.firstOrNull { it.propertyValue == value } ?: PORTRAIT
    }
}

// a project is an .aproj properties file plus the directory tree derived from its location
class Project private constructor(val configFile: File, private val properties: Properties) {

    val rootDirectory: File =
        requireNotNull(configFile.parentFile) { "Project config has no parent directory" }

    val sourcesDirectory: File get() = rootDirectory.resolve(ProjectLayout.SOURCE_DIRECTORY)
    val librariesDirectory: File get() = rootDirectory.resolve(ProjectLayout.LIBRARIES_DIRECTORY)
    val resourcesDirectory: File get() = rootDirectory.resolve(ProjectLayout.RESOURCES_DIRECTORY)
    val binariesDirectory: File get() = rootDirectory.resolve(ProjectLayout.BINARY_DIRECTORY)
    val buildDirectory: File get() = rootDirectory.resolve(ProjectLayout.BUILD_DIRECTORY)

    val mainModuleName: String
        get() = properties.getProperty(KEY_MAIN_MODULE, "")

    val metadata: MidletMetadata
        get() =
            MidletMetadata(
                name = properties.getProperty(KEY_NAME, DEFAULT_NAME),
                vendor = properties.getProperty(KEY_VENDOR, DEFAULT_VENDOR),
                version = properties.getProperty(KEY_VERSION, DEFAULT_VERSION)
            )

    val compilerSettings: CompilerSettings
        get() =
            CompilerSettings(
                mathType = properties.getProperty(KEY_MATH_TYPE)?.toIntOrNull() ?: DEFAULT_MATH_TYPE,
                canvasType =
                    properties.getProperty(KEY_CANVAS_TYPE)?.toIntOrNull() ?: DEFAULT_CANVAS_TYPE
            )

    val apkKeyboardEnabled: Boolean
        get() =
            properties.getProperty(KEY_APK_KEYBOARD)?.toBooleanStrictOrNull()
                ?: DEFAULT_APK_KEYBOARD

    val apkOrientation: ApkOrientation
        get() = ApkOrientation.fromProperty(properties.getProperty(KEY_APK_ORIENTATION))

    val mainModule: File
        get() {
            require(ProjectNames.isValidModuleName(mainModuleName)) {
                "Invalid main module name: $mainModuleName"
            }

            return sourcesDirectory.requireDirectChildName(
                mainModuleName + ProjectLayout.PASCAL_EXTENSION
            )
        }

    val outputJar: File
        get() =
            binariesDirectory.requireDirectChildName(
                metadata.name + ProjectLayout.JAR_EXTENSION
            )

    val outputApk: File
        get() =
            binariesDirectory.requireDirectChildName(
                metadata.name + ProjectLayout.APK_EXTENSION
            )

    // the android package name of an exported apk: two midlets that shared one could not be
    // installed side by side, so a project that never had one gets it derived from its name
    val packageName: String
        get() =
            properties.getProperty(KEY_PACKAGE)
                ?.takeIf(ProjectNames::isValidPackageName)
                ?: ProjectNames.defaultPackageName(metadata.name)

    fun updateMetadata(metadata: MidletMetadata) {
        require(ProjectNames.isValidMetadata(metadata)) { "Invalid MIDlet metadata" }
        properties.setProperty(KEY_NAME, metadata.name)
        properties.setProperty(KEY_VENDOR, metadata.vendor)
        properties.setProperty(KEY_VERSION, metadata.version)
    }

    fun updatePackageName(name: String) {
        require(ProjectNames.isValidPackageName(name)) { "Invalid package name: $name" }
        properties.setProperty(KEY_PACKAGE, name)
    }

    fun updateApkKeyboard(enabled: Boolean) {
        properties.setProperty(KEY_APK_KEYBOARD, enabled.toString())
    }

    fun updateApkOrientation(orientation: ApkOrientation) {
        properties.setProperty(KEY_APK_ORIENTATION, orientation.propertyValue)
    }

    fun updateMainModule(name: String) {
        require(ProjectNames.isValidModuleName(name)) { "Invalid main module name: $name" }
        properties.setProperty(KEY_MAIN_MODULE, name)
    }

    fun isFixedEntry(file: File): Boolean =
        file == configFile ||
                file == binariesDirectory ||
                file == sourcesDirectory ||
                file == librariesDirectory ||
                file == resourcesDirectory ||
                file == buildDirectory

    fun createDirectoryStructure() {
        listOf(
            binariesDirectory,
            sourcesDirectory,
            librariesDirectory,
            resourcesDirectory,
            buildDirectory
        ).forEach(File::createDirectories)
    }

    fun save() {
        configFile.writeAtomically { output ->
            properties.store(output, null)
        }
    }

    companion object {
        fun open(configFile: File): Project =
            Project(configFile, Properties().apply { configFile.inputStream().use(::load) })
                .also { project -> project.mainModule }

        fun create(configFile: File, name: String): Project =
            Project(configFile, Properties()).apply {
                properties.setProperty(
                    KEY_MAIN_MODULE,
                    ProjectNames.defaultMainModuleName(name)
                )
                properties.setProperty(KEY_MATH_TYPE, DEFAULT_MATH_TYPE.toString())
                properties.setProperty(KEY_CANVAS_TYPE, DEFAULT_CANVAS_TYPE.toString())
                properties.setProperty(KEY_APK_KEYBOARD, DEFAULT_APK_KEYBOARD.toString())
                properties.setProperty(
                    KEY_APK_ORIENTATION,
                    ApkOrientation.PORTRAIT.propertyValue
                )
                updateMetadata(MidletMetadata(name, DEFAULT_VENDOR, "1.0"))
            }

        private const val KEY_MAIN_MODULE = "MainModule"
        private const val KEY_PACKAGE = "Package"
        private const val KEY_APK_KEYBOARD = "ApkKeyboard"
        private const val KEY_APK_ORIENTATION = "ApkOrientation"
        private const val KEY_MATH_TYPE = "MathType"
        private const val KEY_CANVAS_TYPE = "CanvasType"
        private const val KEY_NAME = "Name"
        private const val KEY_VENDOR = "Vendor"
        private const val KEY_VERSION = "Version"

        private const val DEFAULT_NAME = "app"
        private const val DEFAULT_VENDOR = "vendor"
        private const val DEFAULT_VERSION = "1"
        private const val DEFAULT_MATH_TYPE = 0
        private const val DEFAULT_CANVAS_TYPE = 1
        private const val DEFAULT_APK_KEYBOARD = false
    }
}
