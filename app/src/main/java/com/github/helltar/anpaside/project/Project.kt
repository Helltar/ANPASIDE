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

/**
 * Everything an exported apk carries that the MIDlet manifest does not decide.
 *
 * [label] and [versionCode] are optional overrides, because both used to be taken from the MIDlet
 * metadata and that name does four jobs at once - it is the MIDlet-Name, the name of the jar, the
 * name of the apk and the name under the launcher icon. A blank label keeps the old behaviour,
 * and so does a null version code.
 */
data class ApkSettings(
    val packageName: String,
    val label: String,
    val versionCode: Int?,
    val iconBackground: String,
    val orientation: ApkOrientation,
    val keyboardEnabled: Boolean
) {

    val iconBackgroundColor: Int
        get() = HexColor.parse(iconBackground) ?: DEFAULT_ICON_BACKGROUND_COLOR

    fun labelOr(midletName: String): String = label.ifBlank { midletName }

    companion object {
        // the tile every exported icon used to sit on, when it was a colour resource of the
        // player template rather than a layer the export draws
        const val DEFAULT_ICON_BACKGROUND = "#3F444C"

        val DEFAULT_ICON_BACKGROUND_COLOR = HexColor.parse(DEFAULT_ICON_BACKGROUND)!!
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

    val apkSettings: ApkSettings
        get() =
            ApkSettings(
                packageName = packageName,
                label = properties.getProperty(KEY_APP_LABEL).orEmpty().trim(),
                // a version code below one cannot be installed, so a broken value is treated as
                // absent and the code is derived from the MIDlet version again
                versionCode =
                    properties.getProperty(KEY_APK_VERSION_CODE)
                        ?.toIntOrNull()
                        ?.takeIf { code -> code >= MIN_VERSION_CODE },
                iconBackground =
                    properties.getProperty(KEY_ICON_BACKGROUND)
                        ?.takeIf(HexColor::isValid)
                        ?: ApkSettings.DEFAULT_ICON_BACKGROUND,
                orientation =
                    ApkOrientation.fromProperty(properties.getProperty(KEY_APK_ORIENTATION)),
                keyboardEnabled =
                    properties.getProperty(KEY_APK_KEYBOARD)?.toBooleanStrictOrNull()
                        ?: DEFAULT_APK_KEYBOARD
            )

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

    fun updateApkSettings(settings: ApkSettings) {
        require(ProjectNames.isValidApkSettings(settings)) { "Invalid APK settings" }
        updatePackageName(settings.packageName)
        properties.setProperty(KEY_APP_LABEL, settings.label)
        properties.setProperty(KEY_ICON_BACKGROUND, settings.iconBackground)
        properties.setProperty(KEY_APK_ORIENTATION, settings.orientation.propertyValue)
        properties.setProperty(KEY_APK_KEYBOARD, settings.keyboardEnabled.toString())

        // an absent key is what makes the version code follow the MIDlet version, so clearing
        // the field has to remove it rather than write something out. not an elvis over let:
        // setProperty answers with the previous value, which is null for a key being added, and
        // that would take the branch that deletes what was just written
        val versionCode = settings.versionCode

        if (versionCode == null) {
            properties.remove(KEY_APK_VERSION_CODE)
        } else {
            properties.setProperty(KEY_APK_VERSION_CODE, versionCode.toString())
        }
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
                properties.setProperty(
                    KEY_ICON_BACKGROUND,
                    ApkSettings.DEFAULT_ICON_BACKGROUND
                )
                // AppLabel and ApkVersionCode are left out on purpose: absent means "follow the
                // MIDlet metadata", which is what a new project wants
                updateMetadata(MidletMetadata(name, DEFAULT_VENDOR, "1.0"))
            }

        const val MIN_VERSION_CODE = 1

        private const val KEY_MAIN_MODULE = "MainModule"
        private const val KEY_PACKAGE = "Package"
        private const val KEY_APP_LABEL = "AppLabel"
        private const val KEY_APK_VERSION_CODE = "ApkVersionCode"
        private const val KEY_ICON_BACKGROUND = "IconBackground"
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
