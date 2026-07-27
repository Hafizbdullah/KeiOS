plugins {
    id("com.android.library")
}

android {
    namespace = "os.kei.core.versioning"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.10")
    testImplementation("junit:junit:4.13.2")
}
