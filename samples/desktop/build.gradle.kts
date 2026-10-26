import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The gallery in a desktop window. Run it with ./gradlew :samples:desktop:run.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.roborazzi)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":samples:gallery"))
            implementation(compose.desktop.currentOs)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.roborazzi.compose.desktop)
            implementation(kotlin("test"))
        }
    }
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/jvmTest/screenshots"))
}

tasks.named<Test>("jvmTest") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

compose.desktop {
    application {
        mainClass = "com.jantursky.stipple.samples.desktop.MainKt"
    }
}
