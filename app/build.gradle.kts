import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.ngth1010v.mydailynote"
    compileSdk = 37
    // Same NDK as speech:whisper; needed to strip debug symbols from native libraries.
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "io.github.ngth1010v.mydailynote"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Only ship the languages the app actually has strings for.
    androidResources {
        localeFilters += listOf("en", "vi")
    }

    // Release key from the gitignored keystore.properties (CI writes it from repository secrets);
    // without it the release build falls back to the debug key, so a fresh clone still builds.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.let { f -> Properties().apply { f.inputStream().use(::load) } }
    signingConfigs {
        if (keystoreProps != null) create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (keystoreProps != null) "release" else "debug")
        }
    }

    // Release: one APK per CPU type plus a universal one (Vosk/whisper native code is per ABI).
    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Vosk ships x86 (32-bit) libraries too; no supported device needs them.
            excludes += "lib/x86/**"
        }
    }
}

baselineProfile {
    // The committed profile in src/main/generated is the source of truth; regenerate it with
    // `gradlew :app:generateBaselineProfile` and an API 33+ emulator connected (English UI).
    saveInSrc = true
    automaticGenerationDuringBuild = false
    mergeIntoMain = true
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:settings"))
    implementation(project(":speech:api"))
    implementation(project(":speech:vosk"))
    implementation(project(":speech:whisper"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Installs the baseline profile on sideloaded (GitHub) installs too.
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("com.alphacephei:vosk-android:${libs.versions.vosk.get()}")
}
