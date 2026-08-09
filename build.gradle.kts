// FloatMaster build configuration.
// WHY: Keep the toolchain on versions supported by Android 17/API 37 while using AGP 9's built-in Kotlin support.
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.kapt") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}
