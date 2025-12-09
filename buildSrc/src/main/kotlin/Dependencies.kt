/****************************************************************************
 *
 * Created by menion on 11.2.2022.
 * Copyright (c) 2022. All rights reserved.
 *
 * This file is part of the Asamm team software.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 ***************************************************************************/

//*****************************************************
// PROJECT
//*****************************************************

object Modules {
    const val common = ":common"
    const val device = ":device"
    const val wear = ":wear"
}

// version parameters
private const val PARAM_MAJOR_VERSION = "1"
private const val PARAM_MINOR_VERSION = "06"
private const val PARAM_PATCH_VERSION = "003"

private const val PARAM_DEVICE_VERSION_SUFFIX = "1"
private const val PARAM_WEAR_VERSION_SUFFIX = "0"

private const val PARAM_VERSION_NAME_READABLE = "1.6.0"

object ModuleDevice {

    val versionCode = "$PARAM_MAJOR_VERSION$PARAM_MINOR_VERSION$PARAM_PATCH_VERSION$PARAM_DEVICE_VERSION_SUFFIX".toInt()

    const val versionName = PARAM_VERSION_NAME_READABLE
}

object ModuleWear {

    val versionCode = "$PARAM_MAJOR_VERSION$PARAM_MINOR_VERSION$PARAM_PATCH_VERSION$PARAM_WEAR_VERSION_SUFFIX".toInt()

    const val versionName = PARAM_VERSION_NAME_READABLE
}

//*****************************************************
// VERSIONS
//*****************************************************

/**
 * Core versions needed by build system or global project setup.
 */
object Versions {

    // BUILD

    // https://developer.android.com/studio/releases/gradle-plugin.html
    // https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google
    // change also: buildSrc/build.gradle.kts
    const val gradle = "8.5.2"

    // https://developer.android.com/studio/releases/build-tools.html
    const val buildTools = "35.0.0"

    // ANDROID

    const val compileSdk = 36
    const val minSdk = 23
    const val targetSdk = 35

    const val minSdkWear = 25
    const val targetSdkWear = 33

    // KOTLIN

    // https://github.com/JetBrains/kotlin
    const val kotlin = "2.2.20"
}

/**
 * Internal libraries.
 */
private object VersionsApi {
    // Locus API
    const val locusApi = "0.9.71"

    // Logger (Asamm)
    const val logger = "2.2"
}

/**
 * Versions for AndroidX dependencies
 */
object VersionsAndroidX {
    // https://developer.android.com/jetpack/androidx/releases/activity
    const val activity = "1.12.1"
    const val appCompat = "1.7.1"
    // https://developer.android.com/training/wearables/compose
    const val compose = "1.2.1"
    // https://developer.android.com/jetpack/androidx/releases/compose-compiler
    const val composeCompiler = "1.5.3"
    const val constraintLayout = "2.2.1"
    const val splashScreen = "1.2.0"
    // https://developer.android.com/jetpack/androidx/releases/wear
    const val wear = "1.3.0"
    const val wearInput = "1.2.0"
    const val wearOngoing = "1.1.0"
    const val wearPhoneInteraction = "1.1.0"
    const val wearRemoteInteractions = "1.1.0"
}

/**
 * Versions for Google based libraries.
 */
private object VersionsGoogle {
    // https://developer.android.com/training/wearables/data/data-layer
    // https://developers.google.com/android/guides/setup?device=wear-os#dependencies
    const val psWear = "19.0.0"

    // https://github.com/material-components/material-components-android/releases
    const val material = "1.13.0"
}

//*****************************************************
// LIBRARIES
//*****************************************************

object Build {
    const val androidGradle = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
}

object Libraries {
    const val androidXAppCompat = "androidx.appcompat:appcompat:${VersionsAndroidX.appCompat}"
    const val composeActivity = "androidx.activity:activity-compose:${VersionsAndroidX.activity}"
    const val composeLiveData = "androidx.compose.runtime:runtime-livedata:${VersionsAndroidX.compose}"
    const val composeMaterial = "androidx.wear.compose:compose-material:${VersionsAndroidX.compose}"
    const val composeTooling = "androidx.compose.ui:ui-tooling:${VersionsAndroidX.compose}"
    const val composeToolingPreview = "androidx.compose.ui:ui-tooling-preview:${VersionsAndroidX.compose}"
    const val androidXConstraintLayout = "androidx.constraintlayout:constraintlayout:${VersionsAndroidX.constraintLayout}"
    const val androidXSplashScreen = "androidx.core:core-splashscreen:${VersionsAndroidX.splashScreen}"
    const val androidXWear = "androidx.wear:wear:${VersionsAndroidX.wear}"
    const val androidXWearInput = "androidx.wear:wear-input:${VersionsAndroidX.wearInput}"
    const val androidXWearOngoing = "androidx.wear:wear-ongoing:${VersionsAndroidX.wearOngoing}"
    const val androidXWearPhoneInteraction = "androidx.wear:wear-phone-interactions:${VersionsAndroidX.wearPhoneInteraction}"
    const val androidXWearRemoteInteractions = "androidx.wear:wear-remote-interactions:${VersionsAndroidX.wearRemoteInteractions}"
    const val googleMaterial = "com.google.android.material:material:${VersionsGoogle.material}"
    const val googlePsWear = "com.google.android.gms:play-services-wearable:${VersionsGoogle.psWear}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    const val locusApiAndroid = "com.asamm:locus-api-android:${VersionsApi.locusApi}"
    const val locusApiLogger = "com.github.asamm:logger-asamm:${VersionsApi.logger}"
}