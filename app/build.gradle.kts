plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.noor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.noor"
        minSdk = 26
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
    // Dependencies moved to toml file
    implementation(libs.androidx.compose.animation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.amplify.aws.auth.cognito)
    implementation(libs.amplify.core)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.datastore.preferences)

    // Dependencies already in toml file
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // WorkManager with Coroutines
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.compose.material)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    androidTestImplementation(libs.androidx.work.testing)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Retrofit & Moshi
    implementation(libs.retrofit)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.retrofit.converter.moshi)
    ksp(libs.moshi.kotlin.codegen)

    // OkHttp & Logging
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Gson Converter
    implementation(libs.converter.gson)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Room testing
    testImplementation(libs.androidx.room.testing)

    // Kotlin Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)

    // MockK for testing
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent.jvm)

    // Turbine for Flow testing
    testImplementation(libs.turbine)

    // Coil
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Gson
    implementation(libs.gson)

    // Material Icons
    implementation(libs.androidx.material.icons.extended)

    // Adaptive Navigation
    implementation(libs.androidx.adaptive.navigation)

    // ML Kit for OCR (Text Recognition)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Camera for image capture
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

}

//dependencies {
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}