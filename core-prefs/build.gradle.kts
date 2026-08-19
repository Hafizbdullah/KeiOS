plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "os.kei.core.prefs"
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
    implementation(project(":core-concurrency"))
    implementation(project(":core-log"))

    api(libs.mmkv)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinTest)
    testImplementation(libs.junit4)
}
