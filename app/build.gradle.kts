@file:Suppress("DEPRECATION")

import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.invoke

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.hisabapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hisabapp"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
        viewBinding = true
        dataBinding = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Updated version for compatibility with Kotlin 1.9.x
    }

    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildToolsVersion = "36.0.0 rc1"
    ndkVersion = "29.0.14033849 rc4"
}

dependencies {
    // Core Android & Lifecycle
    implementation(libs.androidx.core.ktx) // Version managed by libs
    implementation(libs.androidx.lifecycle.runtime.ktx) // Version managed by libs
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Jetpack Compose
    // The BOM (Bill of Materials) manages versions for most Compose libraries.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")
    implementation("androidx.compose.material:material-icons-core-android")
    implementation("androidx.compose.ui:ui:1.7.0") // Explicit version from dependencies 1
    implementation("androidx.activity:activity-compose:1.9.2") // Explicit version from dependencies 1
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation(libs.androidx.activity.compose) // Version managed by libs

    // Compose Tooling for Previews
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui)
    implementation(libs.ui)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Traditional Android Views
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Room Database
    // Provides an abstraction layer over SQLite for local data storage.
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation("androidx.room:room-runtime:2.6.1") // Explicit version from dependencies 1
    implementation("androidx.room:room-ktx:2.6.1") // Explicit version from dependencies
    //noinspection KaptUsageInsteadOfKsp
    ksp("androidx.room:room-compiler:2.6.1")
    ksp("androidx.room:room-compiler:2.7.0") // Annotation processor

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Image Loading
    // Coil is a coroutine-based image loading library for Android.
    implementation("io.coil-kt:coil-compose:2.7.0") // Higher version from dependencies 1
    implementation("io.coil-kt:coil-compose:2.6.0") // Lower version from dependencies 2

    // UI Utility Libraries
    // A library for creating circular ImageViews.
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // A powerful charting library.
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Glance for App Widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Testing
    testImplementation(libs.junit) // Unit testing
    androidTestImplementation(libs.androidx.junit) // Instrumentation testing
    androidTestImplementation(libs.androidx.espresso.core) // UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Compose UI testing

    // Add the Compose BOM for your Android tests
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Now this dependency will have its version resolved by the BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4") // Compose UI testing

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}
