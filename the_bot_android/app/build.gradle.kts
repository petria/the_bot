plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI can provide google-services.json as a protected workspace file. Local builds
// remain possible without Firebase project metadata.
if (file("src/dev/google-services.json").exists() || file("src/prod/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "org.freakz.botclient"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "org.freakz.botclient"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "instance"
    productFlavors {
        create("dev") {
            dimension = "instance"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "BOT_WEB_BASE_URL", "\"https://hokandevbot.airiot.fi\"")
        }
        create("prod") {
            dimension = "instance"
            buildConfigField("String", "BOT_WEB_BASE_URL", "\"https://hokanthebot.airiot.fi\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
}
