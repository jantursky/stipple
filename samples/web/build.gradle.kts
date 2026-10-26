import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The gallery in the browser, compiled to Kotlin/Wasm. Build it with ./gradlew :samples:web:wasmJsBrowserDistribution
// or run it with ./gradlew :samples:web:wasmJsBrowserDevelopmentRun.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val siteAssets = layout.buildDirectory.dir("generated/site")
val generateSiteAssets by tasks.registering {
    val brand = rootProject.layout.projectDirectory.dir("design/brand")
    val output = siteAssets
    inputs.dir(brand)
    outputs.dir(output)
    doLast {
        val out = output.get().asFile.apply { deleteRecursively() }
        out.mkdirs()
        brand.file("icon.svg").asFile.copyTo(out.resolve("favicon.svg"))
        brand.file("mark.svg").asFile.copyTo(out.resolve("mark.svg"))
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "web"
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            resources.srcDir(generateSiteAssets)
            dependencies {
                implementation(project(":samples:gallery"))
            }
        }
    }
}
