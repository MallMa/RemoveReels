plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.removereels.android.application.get().pluginId
            implementationClass = "io.github.mallma.removereels.buildlogic.AndroidApplicationConventionPlugin"
        }
    }
}
