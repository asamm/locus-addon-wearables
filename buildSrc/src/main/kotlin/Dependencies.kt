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
private const val PARAM_MINOR_VERSION = "02"
private const val PARAM_PATCH_VERSION = "000"
private const val PARAM_DEVICE_VERSION_SUFFIX = "1"
private const val PARAM_WEAR_VERSION_SUFFIX = "0"

object ModuleDevice {

    val versionCode = "$PARAM_MAJOR_VERSION$PARAM_MINOR_VERSION$PARAM_PATCH_VERSION$PARAM_DEVICE_VERSION_SUFFIX".toInt()

    const val versionName = "$PARAM_MAJOR_VERSION.$PARAM_MINOR_VERSION.$PARAM_PATCH_VERSION"
}

object ModuleWear {

    val versionCode = "$PARAM_MAJOR_VERSION$PARAM_MINOR_VERSION$PARAM_PATCH_VERSION$PARAM_WEAR_VERSION_SUFFIX".toInt()

    const val versionName = "$PARAM_MAJOR_VERSION.$PARAM_MINOR_VERSION.$PARAM_PATCH_VERSION"
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
    const val gradle = "7.1.0"
    // https://developer.android.com/studio/releases/build-tools.html
    const val buildTools = "32.0.0"

    // ANDROID

    const val compileSdk = 31
    const val minSdk = 21
    const val targetSdk = 31

    const val minSdkWear = 23
    const val targetSdkWear = 30

    // KOTLIN

    // https://github.com/JetBrains/kotlin
    const val kotlin = "1.6.10"
}

/**
 * Internal libraries.
 */
private object VersionsApi {
    // Locus API
    const val locusApi = "0.9.45"
}

/**
 * Versions for AndroidX dependencies
 */
private object VersionsAndroidX {
    const val appCompat = "1.4.1"
    const val constraintLayout = "2.1.3"
    const val wear = "1.2.0"
}

/**
 * Versions for Google based libraries.
 */
private object VersionsGoogle {
    const val psWear = "17.1.0"
    const val material = "1.4.0"
    const val supportWear = "2.8.1"
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
    const val androidXConstraintLayout = "androidx.constraintlayout:constraintlayout:${VersionsAndroidX.constraintLayout}"
    const val androidXWear = "androidx.wear:wear:${VersionsAndroidX.wear}"
    const val googleMaterial = "com.google.android.material:material:${VersionsGoogle.material}"
    const val googlePsWear = "com.google.android.gms:play-services-wearable:${VersionsGoogle.psWear}"
    const val googleSupportWearable = "com.google.android.support:wearable:${VersionsGoogle.supportWear}"
    const val googleWearable = "com.google.android.wearable:wearable:${VersionsGoogle.supportWear}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    const val locusApiAndroid = "com.asamm:locus-api-android:${VersionsApi.locusApi}"
}