plugins {
    id("com.android.library")
}

android {
    namespace = "os.kei.feature.github.engine"
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
    implementation(project(":core-io"))
    implementation(project(":core-json"))
    implementation(project(":core-versioning"))

    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
    testImplementation("junit:junit:4.13.2")
}
