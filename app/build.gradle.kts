plugins {
    `android-application`
    kotlin("android")
    kotlin("kapt")
    hilt
}
androidAppConfig {
    defaultConfig {
        applicationId = "com.elbehiry.coble"
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures{
        dataBinding = true
    }
}

dependencies {
    implementation(project(":coBle"))
    implementation(Dependencies.activity)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.material)
    implementation(Dependencies.constraintlayout)
    implementation(Dependencies.hiltAndroid)
    kapt(Dependencies.hiltCompiler)
    implementation(Dependencies.Lifecycle.runtime)
    kapt(Dependencies.Lifecycle.compiler)
}