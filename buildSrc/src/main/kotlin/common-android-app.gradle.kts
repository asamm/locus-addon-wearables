plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    compileSdk = Versions.compileSdk
    buildToolsVersion = Versions.buildTools

    // enable support for Java 8
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Kotlin compiler, target JVM
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // lint setup
    lint {
        // set to true to have all release builds run lint on issues with severity=fatal
        // and abort the build (controlled by abortOnError above) if fatal issues are found
        checkReleaseBuilds = true
        // if true, stop the gradle build if errors are found
        abortOnError = true
    }

    packagingOptions {
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE.txt")
    }

    // signing of versions
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("ANDROID_SIGN_RELEASE").split("|")[0])
            storePassword = System.getenv("ANDROID_SIGN_RELEASE").split("|")[1]
            keyAlias = System.getenv("ANDROID_SIGN_RELEASE").split("|")[2]
            keyPassword = System.getenv("ANDROID_SIGN_RELEASE").split("|")[3]
        }
    }

    // building task
    // https://google.github.io/android-gradle-dsl/current/com.android.build.gradle.internal.dsl.BuildType.html
    buildTypes {
        getByName("debug") {
            // signing parameters
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            // signing parameters
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    // modules
    implementation(project(Modules.common))
}
