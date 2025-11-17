// ✅ Root-level build.gradle.kts

plugins {
    // Do NOT apply kapt here!
    // Just include the Android and Kotlin plugins management
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    // ✅ Add this line for kapt plugin version management
    id("org.jetbrains.kotlin.kapt") version "1.9.25" apply false
    // ✅ Add Hilt plugin
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
