import java.util.Properties

// Compile-only stand-ins for the few @SystemApi classes FlipSlop uses
// (TrustAgentService). The public android.jar Gradle compiles against
// leaves them out; on the device, and in the Soong build (sdk_version
// "system_current"), the real framework classes are used. Nothing here
// is packaged into the app.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val sdkDir: String = Properties().run {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
    getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        ?: error("Android SDK not found: set sdk.dir in local.properties or ANDROID_HOME")
}

dependencies {
    compileOnly(files("$sdkDir/platforms/android-36/android.jar"))
}
