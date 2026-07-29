plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Puts the player module's apk into this module's assets, where the apk exporter reads it as
 * the template for every exported midlet.
 *
 * The player is a separate application module, so its apk cannot be consumed as an ordinary
 * dependency; it is taken from its output directory after `:player:assembleRelease` ran.
 */
abstract class BundlePlayerTemplate : DefaultTask() {

    @get:InputDirectory
    abstract val playerOutputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val assetDirectory: DirectoryProperty

    @TaskAction
    fun bundle() {
        val outputs = playerOutputDirectory.get().asFile
        val apk =
            outputs.listFiles().orEmpty().singleOrNull { it.name.endsWith(".apk") }
                ?: error("Expected exactly one player apk in $outputs")

        val target = assetDirectory.get().asFile.resolve("player")
        target.mkdirs()
        apk.copyTo(target.resolve("template.apk"), overwrite = true)
    }
}

val bundlePlayerTemplate =
    tasks.register<BundlePlayerTemplate>("bundlePlayerTemplate") {
        dependsOn(":player:assembleRelease")
        playerOutputDirectory.set(
            project(":player").layout.buildDirectory.dir("outputs/apk/release")
        )
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
        versionCode = 34
        versionName = "2.0.0"
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
