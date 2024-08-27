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

    // set compile target
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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