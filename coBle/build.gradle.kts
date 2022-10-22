plugins {
    `android-library`
    kotlin("android")
}

androidLibraryConfig()

dependencies {
    implementation(Dependencies.appCompat)
    implementation(Dependencies.Kotlin.coroutines)
    implementation(Dependencies.timber)
}
