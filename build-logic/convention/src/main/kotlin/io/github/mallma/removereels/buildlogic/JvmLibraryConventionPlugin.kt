package io.github.mallma.removereels.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Minimum line coverage of a pure module, enforced by `check`. */
private const val MIN_LINE_COVERAGE_PERCENT = 90

/**
 * Pure Kotlin module: no Android classes on the classpath, explicit API mode, JUnit Jupiter with Truth, a coverage
 * minimum and detekt.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            extensions.configure<KotlinJvmProjectExtension> {
                explicitApi()
            }
            extensions.configure<KoverProjectExtension> {
                reports {
                    verify {
                        rule {
                            minBound(MIN_LINE_COVERAGE_PERCENT)
                        }
                    }
                }
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
            dependencies {
                "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
                "testImplementation"(libs.findLibrary("junit-jupiter").get())
                "testImplementation"(libs.findLibrary("truth").get())
                "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
            }

            configureKotlin()
            configureDetekt()
        }
    }
}
