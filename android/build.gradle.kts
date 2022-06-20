// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
}
buildscript {
    val compose_version by extra("1.0.0-beta08")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.0-alpha05")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
//        classpath("com.google.dagger:hilt-android-gradle-plugin:2.36")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}