plugins {
    // configure base
    `common-android-app`
}

android {
    namespace = "com.asamm.locus.addon.wear"
    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        versionCode = ModuleDevice.versionCode
        versionName = ModuleDevice.versionName
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    wearApp(project(Modules.wear))
}