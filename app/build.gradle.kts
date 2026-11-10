plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.audioscribe.poc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.audioscribe.poc"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LGPL3.0,ASL2.0,proguard-*,*.kotlin_module}"
        }
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "27.0.12077973"

}

dependencies {
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.5")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.2")
    implementation("com.google.android.material:material:1.12.0")
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose UI
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.10")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // OkHttp for model download
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Testing
    testImplementation("junit:junit:4.13.2")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.10")
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.6.10")
}

// Add at END of android { } block (before dependencies { })
// externalNativeBuild {
//     cmake {
//         path = file("src/main/cpp/CMakeLists.txt")
//         version = "3.22.1"
//     }
// }
// 
// ndkVersion = "27.0.12077973"
