import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun readLocalPropertyOrNull(key: String): String? {
    val localPropsFile = rootProject.file("local.properties")
    if (!localPropsFile.exists()) return null
    return runCatching {
        val props = Properties()
        localPropsFile.inputStream().use(props::load)
        props.getProperty(key)
    }.getOrNull()
}

val miuixVersion =
    providers.gradleProperty("miuix.version").orNull
        ?: readLocalPropertyOrNull("miuix.version")
        ?: "0.9.2"
val composeVersion = "1.11.4"
val activityComposeVersion = "1.13.0"
val lifecycleRuntimeComposeVersion = "2.11.0"
val backdropVersion = "2.0.0"
val capsuleVersion = "2.1.3"
val shapesVersion = "1.2.0"
val lucideIconsVersion = "2.2.1"
val projectCompileSdk = 37
val projectMinSdk = 35
val projectJavaVersion = JavaVersion.VERSION_21
val projectJvmTarget = JvmTarget.JVM_21

android {
    namespace = "os.kei.ui.liquidglass"
    compileSdk = projectCompileSdk

    defaultConfig {
        minSdk = projectMinSdk
    }

    compileOptions {
        sourceCompatibility = projectJavaVersion
        targetCompatibility = projectJavaVersion
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkDependencies = false
    }

    compileSdkMinor = 0

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(projectJvmTarget)
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-ui"))
            .using(module("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-icons"))
            .using(module("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-squircle"))
            .using(module("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion"))
        substitute(module("top.yukonga.miuix.kmp:miuix-blur"))
            .using(module("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion"))
    }
}

dependencies {
    api("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleRuntimeComposeVersion")
    api("androidx.compose.ui:ui:$composeVersion")
    api("androidx.compose.foundation:foundation:$composeVersion")
    api("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    api("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    api("io.github.kyant0:backdrop:$backdropVersion")
    api("io.github.kyant0:capsule:$capsuleVersion")
    api("io.github.kyant0:shapes:$shapesVersion")
    api("com.composables:icons-lucide-android:$lucideIconsVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}
