plugins {
    alias(libs.plugins.removereels.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.mallma.removereels"

    defaultConfig {
        applicationId = "io.github.mallma.removereels"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":budget"))
    implementation(project(":detection"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    constraints {
        testImplementation(libs.androidx.test.espresso.core) {
            because("Compose UI tests pull in Espresso 3.5, which fails on Robolectric's Android 17 runtime")
        }
    }
}
