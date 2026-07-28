import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
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
        ?: libs.versions.miuix.get()
val projectCompileSdk = libs.versions.compile.sdk.get().toInt()
val projectMinSdk = libs.versions.min.sdk.get().toInt()
val projectJavaVersion = JavaVersion.toVersion(libs.versions.java.get())
val projectJvmTarget = JvmTarget.fromTarget(libs.versions.java.get())

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
    api(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui.tooling.preview)
    api("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-squircle-android:$miuixVersion")
    api("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    api(libs.kyant.backdrop)
    api(libs.kyant.capsule)
    api(libs.kyant.shapes)
    api(libs.lucide.icons)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
