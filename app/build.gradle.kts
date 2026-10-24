plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maha.office.security"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maha.office.security"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.9.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}
