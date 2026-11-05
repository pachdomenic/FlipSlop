import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// Optional release signing: drop a keystore.properties next to this file
// (storeFile, storePassword, keyAlias, keyPassword). Without it the release
// build is signed with the debug key so it still installs for testing.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Optional platform signing: a platform.properties (same keys) pointing at a
// keystore made from the ROM's platform key (lineage/platform-keystore.sh).
// Signs debug and release, so builds update the platform-signed system app
// over adb and keep ACCESS_SHORTCUTS.
val platformProps = Properties().apply {
    val f = rootProject.file("platform.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.tuki.flipslop"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tuki.flipslop"
        minSdk = 35
        targetSdk = 36
        // Keep in sync with --version-code / --version-name in Android.bp.
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (platformProps.isNotEmpty()) {
            create("platform") {
                storeFile = rootProject.file(platformProps.getProperty("storeFile"))
                storePassword = platformProps.getProperty("storePassword")
                keyAlias = platformProps.getProperty("keyAlias")
                keyPassword = platformProps.getProperty("keyPassword")
            }
        }
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("platform")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("platform")
                ?: signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Not used: lineage/ builds the same sources with Soong, which has no BuildConfig.
        buildConfig = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // Robolectric's SDK 36 sandbox pokes at FileDescriptor internals.
                it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
            }
        }
    }

    lint {
        disable += "ProtectedPermissions"
        // LineageOS 23 is Android 16 (API 36); compileSdk 37 is only for Material 3 Expressive.
        disable += "OldTargetApi"
        // Versions are pinned to prebuilts/sdk of LineageOS 23 on purpose (see libs.versions.toml).
        disable += "GradleDependency"
    }
}

kotlin {
    compilerOptions {
        // AOSP 16 QPR2 builds with kotlinc 2.2.0: no newer language features.
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

dependencies {
    // @SystemApi classes (TrustAgentService); the framework provides them at runtime.
    compileOnly(project(":systemstubs"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.window)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
