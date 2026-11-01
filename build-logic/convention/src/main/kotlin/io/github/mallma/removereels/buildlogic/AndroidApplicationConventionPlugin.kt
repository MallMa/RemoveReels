package io.github.mallma.removereels.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android app module: SDK levels, Java and Kotlin targets, Compose, release shrinking, tests, Lint, detekt and the
 * forbidden-permission check.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.intVersion("androidCompileSdk")
                defaultConfig {
                    minSdk = libs.intVersion("androidMinSdk")
                    targetSdk = libs.intVersion("androidTargetSdk")
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
                buildFeatures {
                    compose = true
                }
                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    }
                }
                testOptions {
                    unitTests.isIncludeAndroidResources = true
                    unitTests.all { test ->
                        // Robolectric's Android 16+ runtime reaches into this JDK package.
                        test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
                    }
                }
                lint {
                    warningsAsErrors = true
                    abortOnError = true
                    // Newer versions arrive as Dependabot pull requests instead of failing the build.
                    disable += setOf("AndroidGradlePluginVersion", "GradleDependency")
                }
            }

            configureKotlin()
            configureDetekt()
            configureManifestPermissionCheck()
        }
    }
}
