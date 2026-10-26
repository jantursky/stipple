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
