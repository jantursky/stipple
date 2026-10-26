#!/usr/bin/env bash
# Publishes the two modules to Maven Local and compiles a fresh consumer project against them for the JVM and
# the browser, so a broken POM, a missing variant or a leaked internal type fails here, not for users.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
compose=$(sed -n 's/^compose = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
trap 'rm -rf "$consumer"' EXIT

"$root/gradlew" -p "$root" :stipple:publishToMavenLocal :stipple-components:publishToMavenLocal --quiet

mkdir -p "$consumer/src/commonMain/kotlin"
cat > "$consumer/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroup("io.github.jantursky") } }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer"
EOF
cat > "$consumer/build.gradle.kts" <<EOF
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "$kotlin"
    id("org.jetbrains.kotlin.plugin.compose") version "$kotlin"
    id("org.jetbrains.compose") version "$compose"
}

kotlin {
    jvm()
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jantursky:stipple:$version")
            implementation("io.github.jantursky:stipple-components:$version")
        }
    }
}
EOF
cat > "$consumer/src/commonMain/kotlin/Consumer.kt" <<'EOF'
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.DitherRenderer
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.ShaderField
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.components.BarEntry
import com.jantursky.stipple.components.DitherBarChart
import com.jantursky.stipple.components.DitherButton
import com.jantursky.stipple.components.DitherProgressBar
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherFill
import com.jantursky.stipple.ditherImage
import com.jantursky.stipple.ditherMask
import com.jantursky.stipple.ditherProgress

object Flat : ShaderField {
    override val shaderSource = "float tone(float2 uv, float t, float aspect) { return 0.5; }"

    override fun tone(x: Float, y: Float, t: Float, aspect: Float) = 0.5f
}

@Composable
fun Screen(progress: Float) {
    ProvideStippleTheme(StippleTheme.Light.copy(cell = 2.dp)) {
        Box(Modifier.size(100.dp).dither(DitherFields.waves(), DitherPatterns.Bayer8, animated = true, renderer = DitherRenderer.Auto))
        Box(Modifier.size(100.dp).dither(Flat, renderer = DitherRenderer.Shader))
        Box(Modifier.size(100.dp).dither(DitherField { _, y, _, _ -> y }))
        Box(Modifier.size(100.dp).ditherProgress({ progress }))
        Box(Modifier.size(100.dp).ditherImage(algorithm = DitherAlgorithm.Atkinson))
        Box(Modifier.size(100.dp).ditherFill(DitherFields.radial()))
        Box(Modifier.size(100.dp).ditherMask(DitherFields.linear()))
        DitherProgressBar(progress)
        DitherButton("Go", onClick = {})
        DitherBarChart(listOf(BarEntry("a", 1f), BarEntry("b", 2f)))
    }
}
EOF

"$root/gradlew" -p "$consumer" compileKotlinJvm compileKotlinWasmJs --quiet
echo "Consumer compiled for jvm and wasmJs against io.github.jantursky:stipple and stipple-components $version"
