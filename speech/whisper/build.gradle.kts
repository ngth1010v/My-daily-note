plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.ngth1010v.mydailynote.speech.whisper"
    ndkVersion = "30.0.16248370"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // 64-bit phones + the x86_64 emulator. 32-bit ARM is left out: whisper is too slow there.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                // whisper is unusably slow without optimizations, so even debug builds use -O3.
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DANDROID_STL=c++_static")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":speech:api"))
}
