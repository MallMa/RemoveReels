package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Runs the bundled rules against screens recorded on a phone, stored as
 * `fixtures/<app>/<app version>/<expected surface, or none>/<screen>.json`.
 */
class BundledRulesTest {
    private val rules = BundledRules.load()
    private val detector = SurfaceDetector(rules)
    private val fixtures = loadFixtures()

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
                    (signals + canary + listOfNotNull(app.homeTabViewId)).map(app::fullViewId)
                }.toSet()

        assertThat(recorded).containsAtLeastElementsIn(named)
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

    private fun loadFixtures(): List<Fixture> {
        val root = File(checkNotNull(javaClass.getResource("/fixtures")).toURI())
        return root
            .walk()
            .filter { it.extension == "json" }
            .map { Fixture(it.relativeTo(root).invariantSeparatorsPath, it.readText()) }
            .sortedBy { it.path }
            .toList()
    }

    private companion object {
        const val NO_SURFACE = "none"
    }
}
