import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kotlin Multiplatform: ta sama domena dla Androida (target jvm) i iOS (framework ClaimsDomain).
// Zero zależności od Androida, Room, Retrofit czy Hilt (reguła z CLAUDE.md).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ClaimsDomain"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
