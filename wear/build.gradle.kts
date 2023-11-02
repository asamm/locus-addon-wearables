plugins {
    // configure base
    `common-android-app`
}

android {
    namespace = "com.asamm.locus.addon.wear"
    defaultConfig {
        minSdk = Versions.minSdkWear
        targetSdk = Versions.targetSdkWear

        versionCode = ModuleWear.versionCode
        versionName = ModuleWear.versionName
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = VersionsAndroidX.composeCompiler
    }
}

dependencies {
    // libraries
    implementation(Libraries.kotlin)
    implementation(Libraries.androidXAppCompat)
    implementation(Libraries.composeActivity)
    implementation(Libraries.composeLiveData)
    implementation(Libraries.composeMaterial)
    implementation(Libraries.composeTooling)
    implementation(Libraries.composeToolingPreview)
    implementation(Libraries.androidXConstraintLayout)
    implementation(Libraries.androidXWear)
    implementation(Libraries.androidXWearRemoteInteractions)
    implementation(Libraries.googleMaterial)
    implementation(Libraries.googleSupportWearable)
    compileOnly(Libraries.googleWearable)
}
