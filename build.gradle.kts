import com.diffplug.spotless.LineEnding

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spotless)
}

/** Files under version control; generated directories are pruned, not just filtered, so they are never walked. */
fun sourceFiles(vararg patterns: String) =
    fileTree(rootDir) {
        include(*patterns)
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/local.properties")
    }

spotless {
    // Same as .gitattributes; deriving it from git at build time breaks the configuration cache.
    lineEndings = LineEnding.UNIX
    kotlin {
        target(sourceFiles("**/*.kt"))
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(sourceFiles("**/*.gradle.kts"))
        ktlint(libs.versions.ktlint.get())
    }
    format("misc") {
        target(sourceFiles("**/*.md", "**/*.xml", "**/*.yml", "**/*.toml", "**/*.properties", ".editorconfig"))
        trimTrailingWhitespace()
        endWithNewline()
    }
}
