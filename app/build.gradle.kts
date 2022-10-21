plugins {
    `android-application`
    kotlin("android")
    kotlin("kapt")
}
androidAppConfig {
    defaultConfig {
        applicationId = "com.elbehiry.coble"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":coBle"))
    implementation(Dependencies.activity)
    implementation(Dependencies.appCompat)
    implementation(Dependencies.material)
    implementation(Dependencies.constraintlayout)
}