plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.3.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
}