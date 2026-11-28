plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Inter (SIL OFL), pinned by release URL and SHA-256. Downloaded at build
// time into a generated res dir so the repo stays text-only and the font
// provenance is verifiable.
val interFontsDir = layout.buildDirectory.dir("generated/res/inter")
val fetchInterFonts = tasks.register("fetchInterFonts") {
    outputs.dir(interFontsDir)
    doLast {
        val zipFile = layout.buildDirectory.file("inter/Inter-4.1.zip").get().asFile
        if (!zipFile.isFile) {
            zipFile.parentFile.mkdirs()
            java.net.URI("https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip")
                .toURL().openStream().use { input ->
                    zipFile.outputStream().use { output -> input.copyTo(output) }
                }
        }
        val md = java.security.MessageDigest.getInstance("SHA-256")
        zipFile.inputStream().use { s ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        val got = md.digest().joinToString("") { "%02x".format(it) }
        check(got == "9883fdd4a49d4fb66bd8177ba6625ef9a64aa45899767dde3d36aa425756b11e") {
            "Inter font download failed its integrity check ($got). Build stopped."
        }
        val wanted = mapOf(
            "extras/ttf/Inter-Regular.ttf" to "inter_regular.ttf",
            "extras/ttf/Inter-Medium.ttf" to "inter_medium.ttf",
            "extras/ttf/Inter-SemiBold.ttf" to "inter_semibold.ttf",
            "extras/ttf/Inter-Bold.ttf" to "inter_bold.ttf",
        )
        val fontDir = File(interFontsDir.get().asFile, "font").apply { mkdirs() }
        java.util.zip.ZipFile(zipFile).use { zip ->
            for ((entryName, outName) in wanted) {
                val entry = zip.getEntry(entryName) ?: error("Missing $entryName in the Inter zip")
                zip.getInputStream(entry).use { input ->
                    File(fontDir, outName).writeBytes(input.readBytes())
                }
            }
        }
    }
}

android {
    namespace = "dev.aicon.studio"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "dev.aicon.studio"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.2.0"

        ndk {
            // Phones run arm64; x86_64 covers emulators.
            abiFilters += listOf("arm64-v8a") // phones only; x86_64 emulator build can return when needed
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDir(interFontsDir)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 built-in Kotlin: jvmTarget defaults from targetCompatibility.
}

tasks.named("preBuild") {
    dependsOn(fetchInterFonts)
}

dependencies {
    implementation(project(":core:engine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
