buildscript {
    repositories.addProjectDefaults()
    dependencies {
        classpath(Dependencies.androidGradlePlugin)
        classpath(Dependencies.Kotlin.plugin)
        classpath(Dependencies.hilt)
    }
}

allprojects {
    repositories.addProjectDefaults()
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
