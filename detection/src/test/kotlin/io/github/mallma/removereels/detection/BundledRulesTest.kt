package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Runs the bundled rules against screens recorded on a phone, stored as
 * `fixtures/<app>/<app version>/<expected surface, or none>/<screen>.json`. Each version folder's `covers.json`
 * lists the entry points that must be covered on its screens.
 */
class BundledRulesTest {
    private val rules = BundledRules.load()
    private val detector = SurfaceDetector(rules)
    private val root = File(checkNotNull(javaClass.getResource("/fixtures")).toURI())
    private val fixtures = loadFixtures()
    private val covers = loadCovers()

    @TestFactory
    fun `every recorded screen shows the surface its folder names`(): List<DynamicTest> =
        fixtures.map { fixture ->
            dynamicTest(fixture.path) {
                val surface = detector.findSurface(fixture.snapshot.packageName, fixture.snapshot)

                assertThat(surface?.name ?: NO_SURFACE).isEqualTo(fixture.expected)
            }
        }

    @TestFactory
    fun `no recorded screen makes the rules look stale`(): List<DynamicTest> =
        fixtures.map { fixture ->
            dynamicTest(fixture.path) {
                assertThat(detector.checkCanary(fixture.snapshot.packageName, fixture.snapshot))
                    .isNotEqualTo(CanaryResult.STALE)
            }
        }

    @Test
    fun `the canary passes on Instagram's feed`() {
        val feed = fixtures.single { it.path.startsWith("instagram/") && it.path.endsWith("/none/feed.json") }

        assertThat(detector.checkCanary(feed.snapshot.packageName, feed.snapshot)).isEqualTo(CanaryResult.HEALTHY)
    }

    @Test
    fun `every surface has recorded screens`() {
        val surfaces = rules.flatMap { app -> app.surfaces.map { it.name } }

        assertThat(fixtures.map { it.expected }).containsAtLeastElementsIn(surfaces)
    }

    @Test
    fun `every view id in the rules appears in a recorded screen`() {
        val recorded = fixtures.flatMap { fixture -> fixture.snapshot.nodes.map { it.viewId } }.toSet()
        val named =
            rules
                .flatMap { app ->
                    val signals = app.surfaces.flatMap { it.anyOf + it.allOf }.map { it.viewId }
                    val canary = listOfNotNull(app.canary?.anchorViewId, app.canary?.expectedViewId)
                    val entryPoints =
                        app.entryPoints.flatMap { listOfNotNull(it.viewId, it.within, it.containing, it.below) }
                    (signals + canary + entryPoints + listOfNotNull(app.homeTabViewId)).map(app::fullViewId)
                }.toSet()

        assertThat(recorded).containsAtLeastElementsIn(named)
    }

    @Test
    fun `each rule list is tested with screens recorded from the version it names`() {
        rules.forEach { app ->
            val versions = fixtures.filter { it.snapshot.packageName == app.packageName }.map { it.snapshot.appVersion }

            assertThat(versions).contains(app.testedVersion)
        }
    }

    @TestFactory
    fun `every listed screen covers exactly its entry points`(): List<DynamicTest> =
        covers.map { (path, expected) ->
            dynamicTest(path) {
                val fixture = fixtures.single { it.path == path }
                val found = detector.findEntryPoints(fixture.snapshot.packageName, fixture.snapshot)

                assertThat(found.map { it.name to it.bounds }).containsExactlyElementsIn(
                    expected.map {
                        it.name to
                            it.bounds
                    },
                )
            }
        }

    @Test
    fun `every screen recorded with its view tree has its covers listed`() {
        val withTree = fixtures.filter { fixture -> fixture.snapshot.nodes.any { it.parent != null } }.map { it.path }

        assertThat(covers.keys).containsAtLeastElementsIn(withTree)
    }

    @TestFactory
    fun `fixtures are stored exactly as the recorder writes them`(): List<DynamicTest> =
        fixtures.map { fixture ->
            dynamicTest(fixture.path) { assertThat(fixture.snapshot.toJson()).isEqualTo(fixture.text) }
        }

    private class Fixture(
        val path: String,
        val text: String,
    ) {
        val expected: String = path.split('/').dropLast(1).last()
        val snapshot = ScreenSnapshot.fromJson(text)
    }

    @Serializable
    private data class ExpectedCover(
        val name: String,
        val bounds: Bounds,
    )

    private fun loadFixtures(): List<Fixture> =
        root
            .walk()
            .filter { it.extension == "json" && it.name != COVERS_FILE }
            .map { Fixture(it.relativeTo(root).invariantSeparatorsPath, it.readText()) }
            .sortedBy { it.path }
            .toList()

    /** Every version folder's covers, keyed by screen path from the fixtures folder. */
    private fun loadCovers(): Map<String, List<ExpectedCover>> =
        root
            .walk()
            .filter { it.name == COVERS_FILE }
            .flatMap { file ->
                val folder = file.parentFile.relativeTo(root).invariantSeparatorsPath
                Json
                    .decodeFromString<Map<String, List<ExpectedCover>>>(
                        file.readText(),
                    ).mapKeys { "$folder/${it.key}" }
                    .entries
            }.associate { it.key to it.value }

    private companion object {
        const val NO_SURFACE = "none"
        const val COVERS_FILE = "covers.json"
    }
}
