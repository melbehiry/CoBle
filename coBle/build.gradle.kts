plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    implementation(Dependencies.Kotlin.coroutines)
}
