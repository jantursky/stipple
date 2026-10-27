import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.roborazzi)
}

/**
 * Turns the Kotlin code fences of the docs site into a Kotlin source file per page, so a docs sample that no longer
 * compiles fails the build. A fence is the body of a composable by default, `kotlin top` puts it at file level and
 * `kotlin nocompile` leaves it out (Gradle configuration, signatures without bodies).
 */
abstract class GenerateDocsSnippets : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docs: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun generate() {
        val root = docs.get().asFile
        val out = output.get().asFile
        out.deleteRecursively()
        val pkgDir = out.resolve("com/jantursky/stipple/docs").apply { mkdirs() }
        val fence =
            Regex("""^```kotlin([^\n]*)\n(.*?)\n```$""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
        var compiled = 0
        var skipped = 0
        val pages =
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "mdx" }
                .sortedBy { it.path }
                .toList()
        for (page in pages) {
            val text = page.readText()
            val rel = page.relativeTo(root).path
            val body = StringBuilder()
            val refs = mutableListOf<String>()
            for ((index, match) in fence.findAll(text).withIndex()) {
                val info =
                    match.groupValues[1]
                        .trim()
                        .split(Regex("\\s+"))
                        .filter { it.isNotEmpty() && '=' !in it }
                if ("nocompile" in info) {
                    skipped++
                    continue
                }
                compiled++
                val line = text.substring(0, match.range.first).count { it == '\n' } + 2
                val name = "snippet$index"
                body
                    .append("\n// ")
                    .append(rel)
                    .append(':')
                    .append(line)
                    .append('\n')
                if ("top" in info) {
                    body.append(match.groupValues[2]).append('\n')
                } else {
                    body.append("@Composable\nprivate fun ").append(name).append("() {\n")
                    match.groupValues[2].lines().forEach { body.append("    ").append(it).append('\n') }
                    body.append("}\n")
                    refs += "::$name"
                }
            }
            if (body.isEmpty()) continue
            val id = rel.removeSuffix(".mdx").replace(Regex("[^A-Za-z0-9]"), "_")
            pkgDir.resolve("Docs_$id.kt").writeText(
                """
                |@file:Suppress("unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER")
                |
                |package com.jantursky.stipple.docs
                |
                |import androidx.compose.animation.core.*
                |import androidx.compose.foundation.*
                |import androidx.compose.foundation.layout.*
                |import androidx.compose.runtime.*
                |import androidx.compose.ui.*
                |import androidx.compose.ui.graphics.*
                |import androidx.compose.ui.unit.*
                |import com.jantursky.stipple.*
                |import com.jantursky.stipple.components.*
                |import kotlin.math.*
                |
                |// Generated from docs/$rel by :stipple-components:generateDocsSnippets. Do not edit.
                |private val target = 0.6f
                |
                |@Composable
                |private fun App() {}
                |
                |@Composable
                |private fun Screen() {}
                |$body
                """.trimMargin(),
            )
        }
        pkgDir.resolve("DocsSnippets.kt").writeText(
            """
            |package com.jantursky.stipple.docs
            |
            |/** How many docs fences were turned into code and how many were left out, for the test to check. */
            |internal object DocsSnippets {
            |    const val COMPILED = $compiled
            |    const val SKIPPED = $skipped
            |}
            """.trimMargin(),
        )
    }
}

val generateDocsSnippets =
    tasks.register<GenerateDocsSnippets>("generateDocsSnippets") {
        docs.set(rootProject.layout.projectDirectory.dir("apps/docs/docs"))
        output.set(layout.buildDirectory.dir("generated/docsSnippets"))
    }

kotlin {
    explicitApi()

    android {
        namespace = "com.jantursky.stipple.components"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest { isIncludeAndroidResources = true }
    }

    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":stipple"))
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.animation)
            implementation(libs.compose.foundation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.robolectric)
        }
        jvmTest {
            kotlin.srcDir(generateDocsSnippets)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.roborazzi.compose.desktop)
            implementation(compose.desktop.currentOs)
        }
    }
}

tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    // Robolectric's newest Android sandbox builds file descriptors through JDK internals that recent JDKs
    // no longer export.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

tasks.named<Test>("jvmTest") {
    // Skiko loads its native library; recent JDKs warn unless native access is granted.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/jvmTest/screenshots"))
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib { enabled = true }
}

kover {
    reports {
        verify {
            rule {
                bound { minValue = 90 }
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                    // The Compose compiler adds a branch per parameter for skipping and defaults. Those are not
                    // behaviour, and every combination of them cannot be driven from a test, so this module gates
                    // lower than the engine does. Lines keep the usual 90.
                    minValue = 55
                }
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Central only needs a javadoc jar to exist. An empty one keeps the upload small: the Dokka HTML was
    // 710 KB per publication, which is most of a release against the monthly size and file limits.
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "stipple-components")
    pom {
        name.set("Stipple Components")
        description.set(
            "Dithered buttons, charts, grids, gauges, stat cards, progress bars, text, avatars, images and " +
                "backgrounds for Compose Multiplatform, built on Stipple.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/jantursky/stipple")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jantursky")
                name.set("Jan Tursky")
                email.set("jan.tursky@gmail.com")
                url.set("https://jantursky.com")
            }
        }
        scm {
            url.set("https://github.com/jantursky/stipple")
            connection.set("scm:git:https://github.com/jantursky/stipple.git")
            developerConnection.set("scm:git:ssh://git@github.com.jantursky.stipple.git")
        }
    }
}

// The wasm and iOS test runs cover commonTest only (pure math, no UI), so they don't need the Skiko
// runtime that this check asks an executable binary for.
tasks.named { it == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
