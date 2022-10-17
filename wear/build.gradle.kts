plugins {
    // configure base
    `common-android-app`
}

android {
    defaultConfig {
        minSdk = Versions.minSdkWear
        targetSdk = Versions.targetSdkWear

        versionCode = ModuleWear.versionCode
        versionName = ModuleWear.versionName
    }
}

dependencies {
    // libraries
    implementation(Libraries.kotlin)
    implementation(Libraries.androidXAppCompat)
    implementation(Libraries.androidXConstraintLayout)
    implementation(Libraries.androidXWear)
    implementation(Libraries.androidXWearRemoteInteractions)
    implementation(Libraries.googleMaterial)
    implementation(Libraries.googleSupportWearable)
    compileOnly(Libraries.googleWearable)
}
