plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.1.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
}