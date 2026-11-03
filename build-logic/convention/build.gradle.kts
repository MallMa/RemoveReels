plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "removereels.android.application"
            implementationClass = "io.github.mallma.removereels.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("jvmLibrary") {
            id = "removereels.jvm.library"
            implementationClass = "io.github.mallma.removereels.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
