buildscript {
    repositories.addProjectDefaults()
    dependencies {
        classpath(Dependencies.androidGradlePlugin)
        classpath(Dependencies.Kotlin.plugin)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    }
}

allprojects {
    repositories.addProjectDefaults()
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
