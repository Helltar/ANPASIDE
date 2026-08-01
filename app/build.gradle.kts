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
        apk.copyTo(target.resolve("template.apk"), overwrite = true)
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
        versionCode = 38
        versionName = "2.3.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
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
