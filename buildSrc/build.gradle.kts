plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.13.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
}