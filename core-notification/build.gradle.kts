plugins {
    id("com.android.library")
}

android {
    namespace = "os.kei.core.notification"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
        consumerProguardFiles("src/main/keepRules/core-notification-rules.keep")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core-log"))
    implementation(project(":core-prefs"))
    implementation(project(":core-system"))

    implementation("androidx.core:core-ktx:1.19.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
