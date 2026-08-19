plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "os.kei.feature.os"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":feature-mcp"))

    testImplementation(libs.kotlinTest)
    testImplementation(libs.junit4)
}
