// Host Android application for the Agent Browser agent stack. The Chromium
// APK lane (chromium-build.yml) produces the full fork; this app module gives
// the hosted CI lane a real, installable browser shell with ABI splits.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.agentbrowser.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agentbrowser.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
    }

    // Release APK split per ABI plus one universal APK, as required by the
    // release lane. Debug-signed so CI always produces installable artifacts;
    // swap in the AB_KEYSTORE_* signing config for store builds.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    // Production signing from AB_KEYSTORE_* (set by release-hosted when
    // secrets are present); falls back to the debug key so CI stays green.
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("AB_KEYSTORE_PATH")
            if (ksPath != null && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("AB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AB_KEY_ALIAS")
                keyPassword = System.getenv("AB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val ksPath = System.getenv("AB_KEYSTORE_PATH")
            signingConfig = if (ksPath != null && file(ksPath).exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ai"))
    implementation(project(":automation"))
    implementation(project(":security"))
    implementation(project(":agent"))
    implementation(project(":scheduler"))
    implementation(project(":browser"))
    implementation(project(":ui"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
