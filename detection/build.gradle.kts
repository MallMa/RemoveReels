plugins {
    alias(libs.plugins.removereels.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
