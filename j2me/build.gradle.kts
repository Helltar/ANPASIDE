// the j2me runtime, taken from J2ME-Loader (apache 2.0) and stripped down to what
// midletpascal programs actually use: lcdui, rms, io, media, sms. no 3d, no camera,
// no app list ui. the namespace is kept as the original one so that the copied
// sources keep resolving ru.playsoftware.j2meloader.R and BuildConfig

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.playsoftware.j2meloader"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
        buildConfigField("boolean", "FULL_EMULATOR", "true")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":dexlib"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.zip4j)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.mididriver)
    implementation(libs.pngj)
}
