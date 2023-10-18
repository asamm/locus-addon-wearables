buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Android build
        classpath(Build.androidGradle)
        // Kotlin
        classpath(Build.kotlin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()

        // package repository for Git
        maven(url = "https://jitpack.io")
    }
}