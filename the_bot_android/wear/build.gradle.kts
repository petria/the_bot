plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "org.freakz.botclient.water"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.freakz.botclient"
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "0.3"
    }
    flavorDimensions += "instance"
    productFlavors {
        create("dev") { dimension = "instance"; applicationIdSuffix = ".dev" }
        create("prod") { dimension = "instance" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
