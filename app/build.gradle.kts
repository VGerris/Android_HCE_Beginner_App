// In /app/build.gradle.kts (the file you were editing)

// This block applies the plugins that were defined in the project-level build script.
// Notice there are NO version numbers here.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.androidcrypto.android_hce_beginner_app"
    compileSdk = 36 // Using 34 as it's the latest stable SDK, 35 is in preview

    defaultConfig {
        applicationId = "de.androidcrypto.android_hce_beginner_app"
        minSdk = 21
        targetSdk = 36 // Match compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Corrected dependencies without using the 'libs' alias for simplicity.
    // Replace with your actual library versions.
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.material.v1110)
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
