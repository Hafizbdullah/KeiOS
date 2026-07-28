plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "os.kei.core.versioning"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit4)
}
