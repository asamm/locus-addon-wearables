plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.2.2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
}