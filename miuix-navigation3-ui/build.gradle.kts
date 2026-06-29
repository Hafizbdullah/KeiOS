plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

fun readLocalPropertyOrNull(key: String): String? {
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) return null
    return runCatching {
        val props = Properties()
        localPropsFile.inputStream().use { input ->
            props.load(input)
        }
        props.getProperty(key)
    }.getOrNull()
}

val miuixVersion =
    providers.gradleProperty("miuix.version").orNull
        ?: readLocalPropertyOrNull("miuix.version")
        ?: "0.9.2"
val composeVersion = "1.11.3"
val activityComposeVersion = "1.13.0"
val lifecycleRuntimeComposeVersion = "2.11.0"
val navigation3Version = "1.1.3"
val navigationEventVersion = "1.1.2"
val androidxCollectionVersion = "1.6.0"

android {
    namespace = "os.kei.libs.miuix.navigation3.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-squircle"))
            .using(module("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion"))
    }
}

dependencies {
    api("androidx.activity:activity-compose:$activityComposeVersion")
    api("androidx.collection:collection:$androidxCollectionVersion")
    api("androidx.compose.animation:animation:$composeVersion")
    api("androidx.compose.foundation:foundation:$composeVersion")
    api("androidx.compose.ui:ui:$composeVersion")
    api("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleRuntimeComposeVersion")
    api("androidx.navigation3:navigation3-runtime:$navigation3Version")
    api("androidx.navigationevent:navigationevent-compose:$navigationEventVersion")
    api("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion")
}
