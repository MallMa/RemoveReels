plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "removereels.android.application"
            implementationClass = "io.github.mallma.removereels.buildlogic.AndroidApplicationConventionPlugin"
        }
    }
}
