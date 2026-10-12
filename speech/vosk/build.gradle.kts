plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.ngth1010v.mydailynote.speech.vosk"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":speech:api"))
    implementation(libs.vosk.android)
}
