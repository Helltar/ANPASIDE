plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 33
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
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
