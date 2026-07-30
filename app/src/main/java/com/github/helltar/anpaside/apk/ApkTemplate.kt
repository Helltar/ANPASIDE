package com.github.helltar.anpaside.apk

/**
 * Identity an exported apk is installed under.
 */
data class ApkApplication(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Int
)

/**
 * A MIDlet version is free text ("1.0", "2.1.3"), Android wants a number that only grows.
 */
object ApkVersions {

    private const val DEFAULT_CODE = 1
    private const val MINOR_SCALE = 100
    private const val MAJOR_SCALE = 10_000

    fun codeOf(versionName: String): Int {
        val parts = versionName.trim().split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return DEFAULT_CODE
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

        return (major.toLong() * MAJOR_SCALE + minor.toLong() * MINOR_SCALE + patch)
            .coerceIn(DEFAULT_CODE.toLong(), Int.MAX_VALUE.toLong())
            .toInt()
    }
}

/**
 * What the ide knows about the player apk it carries in its assets.
 *
 * The placeholders are what the player module is built with, so the two have to be changed
 * together: `player/build.gradle.kts` holds the application id and the version, and
 * `player/src/main/AndroidManifest.xml` the label and the icon resource.
 */
object ApkTemplate {

    const val ASSET_PATH = "player/template.apk"
    const val MANIFEST_ENTRY = "AndroidManifest.xml"
    // both layers of the adaptive icon are pngs so both can be replaced by name; the
    // adaptive-icon xml that ties them together stays as the template built it
    const val ICON_ENTRY = "res/mipmap-xxxhdpi-v4/midlet_icon_foreground.png"
    const val ICON_BACKGROUND_ENTRY = "res/mipmap-xxxhdpi-v4/midlet_icon_background.png"
    const val RESOURCE_TABLE_ENTRY = "resources.arsc"
    const val MIDLET_ASSET_DIRECTORY = "assets/midlet/"

    // the runtime cannot ask for permissions on behalf of an old midlet, and the manifest of
    // the template is what an exported apk is installed with
    const val MIN_SDK = 28

    private const val PLACEHOLDER_PACKAGE = "com.github.helltar.anpaside.midlet"
    private const val PLACEHOLDER_LABEL = "ANPASIDE Midlet"
    private const val PLACEHOLDER_VERSION_NAME = "0.0.0"
    private const val VERSION_CODE_ATTRIBUTE = "versionCode"

    fun patchManifest(manifest: ByteArray, application: ApkApplication): ByteArray {
        val renamed =
            BinaryXml.rewriteStrings(manifest) { value ->
                when {
                    value == PLACEHOLDER_PACKAGE -> application.packageName

                    // the manifest merger derives a permission name and a content provider
                    // authority from the application id, and two exported midlets that kept
                    // the template's could not be installed next to each other
                    value.startsWith("$PLACEHOLDER_PACKAGE.") ->
                        application.packageName + value.removePrefix(PLACEHOLDER_PACKAGE)

                    value == PLACEHOLDER_LABEL -> application.label
                    value == PLACEHOLDER_VERSION_NAME -> application.versionName
                    else -> value
                }
            }

        return BinaryXml.setRootIntAttribute(
            renamed,
            VERSION_CODE_ATTRIBUTE,
            application.versionCode
        )
    }
}
