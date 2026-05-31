//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//}
//
//android {
//    namespace = "com.cubiquitous.tracura"
//    compileSdk {
//        version = release(36)
//    }
//
//    defaultConfig {
//        applicationId = "com.cubiquitous.tracura"
//        minSdk = 24
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // 1. Added old plugins back (Make sure these are in your new libs.versions.toml if using alias)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    // KEEP YOUR NEW PACKAGE NAME HERE
    namespace = "com.cubiquitous.tracura"
    compileSdk = 35 // Kept at 35 to match your old stable dependencies/compiler versions

    defaultConfig {
        // KEEP YOUR NEW APPLICATION ID HERE
        applicationId = "com.cubiquitous.tracura"
        minSdk = 24
        targetSdk = 35
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
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // 2. Brought back Compose and Packaging configs from the old project
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// 3. Brought back your compatibility test task
afterEvaluate {
    tasks.register("testClasses") {
        group = "verification"
        description = "Compiles test classes (compatibility task for Android)"
        val compileTask = tasks.findByName("compileDebugUnitTestKotlin")
        if (compileTask != null) {
            dependsOn(compileTask)
        }
    }
}

dependencies {
    // Core Android / Compose platform libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    implementation(libs.vision.internal.vkp)
    implementation(libs.play.services.maps)
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Firebase (Using BOM 33.16.0)
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database")

    // Utilities, Lifecycle, and UI Extras
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Third-party Libraries (Charts, PDFs, Images)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX & ML Kit
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Testing
    testImplementation(libs.junit)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.0")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}