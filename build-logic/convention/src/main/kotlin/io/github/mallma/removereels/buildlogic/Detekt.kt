package io.github.mallma.removereels.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Static analysis with detekt's defaults plus the project's overrides in config/detekt/detekt.yml. */
internal fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))
    }
    // The plain `detekt` task skips rules that need type resolution, such as ForbiddenMethodCall.
    tasks.named("check").configure {
        dependsOn("detektMain", "detektTest")
    }
}
