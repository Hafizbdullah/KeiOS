plugins {
    id("com.android.library")
}

val liveBenchmarkSystemPropertyKeys =
    listOf(
        "keios.download.liveBenchmark",
        "keios.download.liveUrl",
        "keios.download.liveBytes",
        "keios.download.liveSha256",
        "keios.download.liveRuns",
        "keios.download.maxConnections",
        "keios.download.partMiB",
        "keios.download.controlledBenchmark",
    )

android {
    namespace = "os.kei.core.download"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { test ->
            test.systemProperty("okhttp.platform", "jdk9")
            liveBenchmarkSystemPropertyKeys.forEach { key ->
                providers.systemProperty(key).orNull?.let { value ->
                    test.systemProperty(key, value)
                }
            }
            test.testLogging.showStandardStreams =
                providers.systemProperty("keios.download.liveBenchmark")
                    .orNull
                    ?.equals("true", ignoreCase = true) == true ||
                providers.systemProperty("keios.download.controlledBenchmark")
                    .orNull
                    ?.equals("true", ignoreCase = true) == true
        }
    }
}

dependencies {
    implementation(project(":core-io"))

    api("com.squareup.okhttp3:okhttp:5.4.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
}
