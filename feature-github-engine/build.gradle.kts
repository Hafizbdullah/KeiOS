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
    implementation(project(":core-concurrency"))
    implementation(project(":core-download"))
    implementation(project(":core-io"))
    implementation(project(":core-json"))
    implementation(project(":core-versioning"))

    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
    testImplementation("xmlpull:xmlpull:1.1.3.4d_b4_min")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}
