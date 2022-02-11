plugins {
    // configure base
    `common-android-app`
}

android {
    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        versionCode = ModuleDevice.versionCode
        versionName = ModuleDevice.versionName
    }
}

dependencies {
    wearApp(project(Modules.wear))
}