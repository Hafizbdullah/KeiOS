plugins {
    id("com.android.library")
}

android {
    namespace = "os.kei.feature.webdav"
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
    implementation(project(":core-log"))
    implementation(project(":core-io"))
    implementation(project(":core-prefs"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation(platform("io.ktor:ktor-bom:3.5.1"))
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-encoding")

    // WebDAV client (used by DAVx⁵, production-grade)
    // Exclude xpp3 — Android has built-in XmlPullParser
    implementation("com.github.bitfireAT:dav4jvm:main-SNAPSHOT") {
        exclude(group = "org.ogce", module = "xpp3")
    }
}
