// the template for "export apk": one midlet plus the embedded runtime and nothing of the ide.
// the ide carries this apk in its assets, and for every export it rewrites the package name,
// the label, the version and the launcher icon in a copy of it, then signs the result.
// nothing here is ever installed on its own - the placeholders below only have to be valid

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.helltar.anpaside.player"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // placeholders rewritten in the compiled manifest of every exported apk, so they must
        // stay in sync with ApkTemplate in the ide
        applicationId = "com.github.helltar.anpaside.midlet"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.0"

        ndk {
            // the same set the ide ships its compiler for; without a filter the midi driver
            // alone adds two megabytes of 32 bit x86 to every exported apk
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            optimization {
                // shrinking is what keeps an exported midlet down to a few megabytes: nothing
                // here uses most of appcompat, material or rxjava. src/main/keepRules holds
                // what the midlet reaches by name
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            // compressed native libraries need no page alignment, which keeps the on device
            // rewriting of this archive down to a single aligned entry, resources.arsc
            useLegacyPackaging = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":j2me"))
}
