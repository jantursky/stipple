import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The gallery as a phone and tablet app: GalleryApp from :samples:gallery with the system bars handled. The Android
// app (:samples:androidApp) depends on it; on iOS it is the static GalleryKit framework that samples/iosApp links,
// with MainViewController() as its entry.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.jantursky.stipple.samples.app"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "GalleryKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:gallery"))
        }
    }
}
