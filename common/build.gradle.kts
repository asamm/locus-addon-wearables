plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = Versions.compileSdk
    namespace = "com.asamm.locus.addon.wear.common"

    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk
    }

    // enable support for Java 8
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Kotlin compiler, target JVM
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Kotlin
    implementation(Libraries.kotlin)

    // libraries
    api(Libraries.googlePsWear)
    api(Libraries.locusApiAndroid)
    api(Libraries.locusApiLogger)
}