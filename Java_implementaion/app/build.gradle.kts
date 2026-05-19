plugins {
    application
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.4"

val lwjglNatives: String by lazy {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    when {
        os.contains("windows")                         -> "natives-windows"
        os.contains("mac") && arch.contains("aarch64") -> "natives-macos-arm64"
        os.contains("mac")                             -> "natives-macos"
        else                                           -> "natives-linux"
    }
}

dependencies {
    implementation(libs.guava)
    implementation("org.joml:joml:1.10.7")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")

    runtimeOnly("org.lwjgl", "lwjgl",        classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw",   classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb",    classifier = lwjglNatives)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnit("4.13.2")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

application {
    mainClass = "org.example.App"
    // -XstartOnFirstThread is required on macOS for GLFW to work on the main thread
    applicationDefaultJvmArgs = if (System.getProperty("os.name").lowercase().contains("mac"))
        listOf("-XstartOnFirstThread") else emptyList()
}
