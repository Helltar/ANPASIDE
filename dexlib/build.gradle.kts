// the legacy android dexer, taken from J2ME-Loader's dexlib module (apache 2.0).
// it turns the midlet's java class files into a dex the app can load at runtime

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.dx"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.asm)
    implementation(libs.zip4j)
}
