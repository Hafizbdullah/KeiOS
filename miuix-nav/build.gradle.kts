plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

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
val navigationEventVersion = "1.1.2"
val kotlinxSerializationJsonVersion = "1.11.0"
val kotlinxCollectionsImmutableVersion = "0.5.0"
val lifecycleViewModelComposeVersion = "2.11.0"

android {
    namespace = "top.yukonga.miuix.kmp.nav"
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-squircle"))
            .using(module("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion"))
    }
}

dependencies {
    api("androidx.compose.foundation:foundation:$composeVersion")
    api("androidx.navigationevent:navigationevent-compose:$navigationEventVersion")
    api("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleViewModelComposeVersion")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleViewModelComposeVersion")
    api("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsImmutableVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJsonVersion")
    api("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion")
}
