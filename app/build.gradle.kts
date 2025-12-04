plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}
android {
    namespace = "com.chicken.egglightsaga"
    compileSdk = 36

    defaultConfig {

        val afDevKey: String =
            project.findProperty("APPSFLYER_DEV_KEY") as String? ?: ""

        buildConfigField("String", "AF_DEV_KEY", "\"$afDevKey\"")

        applicationId = "com.chicken.egglightsaga"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.6.1")

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Compose доповнення
    implementation("androidx.compose.material:material:1.6.4")
    implementation("androidx.compose.material:material-icons-extended:1.6.4")
    implementation("androidx.compose.foundation:foundation:1.6.4")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.4")
    implementation("androidx.compose.material3:material3:1.2.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.31.0-alpha")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation("androidx.datastore:datastore-core:1.1.6")

    // Paging
    implementation("androidx.paging:paging-runtime:3.2.0")
    implementation("androidx.paging:paging-compose:1.0.0-alpha19")

    // Test
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation(kotlin("test"))

    // Android Test
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Networking (OkHttp — HTTP клиент)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Attribution / device identifiers
    implementation("com.appsflyer:af-android-sdk:6.12.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")

    // Firebase IDs
    implementation("com.google.firebase:firebase-installations:17.1.0")

    // Play Services (корутины + install referrer)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    implementation("com.android.installreferrer:installreferrer:2.2")

}