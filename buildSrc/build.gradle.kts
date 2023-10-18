plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.3.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
}