plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "os.kei.core.system"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    buildFeatures {
        aidl = true
    }

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        consumerProguardFiles("src/main/keepRules/core-system-rules.keep")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
}

dependencies {
    implementation(project(":core-concurrency"))
    implementation(project(":core-log"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hidden.api.bypass)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinTest)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
