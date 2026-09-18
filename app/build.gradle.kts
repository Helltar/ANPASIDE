import java.nio.ByteBuffer
import java.nio.ByteOrder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// what the player module publishes its apk under; the same attribute is declared there
val artifactType = Attribute.of("com.github.helltar.anpaside.artifact", String::class.java)

val playerApk = configurations.dependencyScope("playerApk")

val playerApkArtifact =
    configurations.resolvable("playerApkArtifact") {
        extendsFrom(playerApk.get())
        attributes {
            attribute(artifactType, "player-apk")
        }
    }

/**
 * Puts the player module's apk into this module's assets, where the apk exporter reads it as
 * the template for every exported midlet.
 *
 * The player is a separate application module, so its apk cannot be consumed as an ordinary
 * dependency; it arrives through the `playerApk` configuration as AGP's own apk artifact, which
 * brings the task that packages it along. This replaced reading `player/build/outputs/apk/release`
 * by path, which stopped the build for good once a Gradle upgrade had emptied that directory while
 * `:player:packageRelease` still reported itself up to date.
 */
abstract class BundlePlayerTemplate : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val playerApkDirectory: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val assetDirectory: DirectoryProperty

    @TaskAction
    fun bundle() {
        // the artifact is a directory: the apk itself, its metadata and any baseline profiles
        val apk =
            playerApkDirectory.asFileTree.matching { include("*.apk") }.files.singleOrNull()
                ?: error("Expected exactly one player apk in ${playerApkDirectory.files}")

        val target = assetDirectory.get().asFile.resolve("player")
        target.mkdirs()
        target.resolve("template.apk").writeBytes(withoutSigningBlock(apk.readBytes()))
    }

    // signing a release from android studio signs every application module, the player included,
    // and an ide that carries a template signed with the release key can not be rebuilt byte for
    // byte by anyone else. the exporter never reads that signature, so it is cut out here: the
    // v2/v3 block sits between the last entry and the central directory, and taking it out only
    // moves the directory offset, which gives exactly the apk an unsigned build produces
    private fun withoutSigningBlock(apk: ByteArray): ByteArray {
        val bytes = ByteBuffer.wrap(apk).order(ByteOrder.LITTLE_ENDIAN)

        // the end of central directory record, which agp writes without a comment
        val record = apk.size - 22
        check(record >= 0 && bytes.getInt(record) == 0x06054b50) { "Unexpected end of the player apk" }

        val directory = bytes.getInt(record + 16)
        val magic = "APK Sig Block 42".toByteArray()

        if (directory < 32 || !apk.copyOfRange(directory - magic.size, directory).contentEquals(magic)) {
            return apk
        }

        // the size is stored on both sides of the block and leaves out its own leading copy
        val size = bytes.getLong(directory - magic.size - 8)
        val start = directory - size.toInt() - 8
        check(start >= 0 && bytes.getLong(start) == size) { "Malformed signing block in the player apk" }

        val result = apk.copyOfRange(0, start) + apk.copyOfRange(directory, apk.size)
        ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).putInt(result.size - 22 + 16, start)
        return result
    }
}

val bundlePlayerTemplate =
    tasks.register<BundlePlayerTemplate>("bundlePlayerTemplate") {
        playerApkDirectory.from(playerApkArtifact)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            bundlePlayerTemplate,
            BundlePlayerTemplate::assetDirectory
        )
    }
}

android {
    namespace = "com.github.helltar.anpaside"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.github.helltar.anpaside"
        minSdk = 28
        targetSdk = 36
        versionCode = 44
        versionName = "2.3.6"
    }

    buildTypes {
        release {
            optimization {
                // most of the dex is compose, material and the bundled dexer, none of it used in
                // full. src/main/keepRules and the j2me consumer rules hold what r8 cannot see
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // the compiler binary is exec'd, so it must reach nativeLibraryDir as a real file on disk;
            // legacy packaging extracts it there at install instead of leaving it packed inside the apk
            useLegacyPackaging = true
        }

        dex {
            // stored dex is the default from minSdk 28 on; deflating it takes the apk from 41 MB
            // to about half that, at the cost of the installer unpacking it on the device
            useLegacyPackaging = true
        }
    }

    dependenciesInfo {
        // the dependency list agp adds to the signing block is encrypted with a key of google's,
        // so nobody else can check what it holds, and izzyondroid flags an apk that carries it
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        // the bundled player apk is already compressed, deflating it again only costs build time
        noCompress += "apk"
    }
}

dependencies {
    // the export template, bundled into the assets by bundlePlayerTemplate above
    add(playerApk.name, project(":player"))

    implementation(project(":j2me"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.zip4j)
    implementation(libs.apksig)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
