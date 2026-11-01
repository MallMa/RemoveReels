package io.github.mallma.removereels.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** Permissions the app promises never to request (docs/adr/0002-no-internet-permission.md). */
private val FORBIDDEN_PERMISSIONS = setOf("android.permission.INTERNET")

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

/** Adds a check per variant that fails when the merged manifest, libraries included, requests a forbidden permission. */
internal fun Project.configureManifestPermissionCheck() {
    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants { variant ->
            val variantName = variant.name.replaceFirstChar(Char::uppercaseChar)
            val checkTask =
                tasks.register<CheckManifestPermissionsTask>("check${variantName}ManifestPermissions") {
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    forbiddenPermissions.set(FORBIDDEN_PERMISSIONS)
                    report.set(layout.buildDirectory.file("reports/manifest-permissions/${variant.name}.txt"))
                }
            tasks.named("check").configure { dependsOn(checkTask) }
        }
    }
}

/** Fails if [mergedManifest] requests any of [forbiddenPermissions]; writes the requested permissions to [report]. */
@CacheableTask
abstract class CheckManifestPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val forbiddenPermissions: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val requested = requestedPermissions()
        val violations = requested intersect forbiddenPermissions.get()
        if (violations.isNotEmpty()) {
            throw GradleException(
                "The merged manifest requests ${violations.sorted().joinToString()}, which the app must never " +
                    "request. See docs/adr/0002-no-internet-permission.md.",
            )
        }
        report.get().asFile.writeText(requested.sorted().joinToString(separator = "\n", postfix = "\n"))
    }

    private fun requestedPermissions(): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        return PERMISSION_TAGS
            .flatMap { tag ->
                val nodes = document.getElementsByTagName(tag)
                (0 until nodes.length).map { index ->
                    (nodes.item(index) as Element).getAttributeNS(ANDROID_NAMESPACE, "name")
                }
            }.toSet()
    }
}
