plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // kotlinx-coroutines-core NO se agrega explicitamente: lo provee IntelliJ Platform
    // https://jb.gg/intellij-platform-kotlin-coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )
        pluginVerifier()
        zipSigner()
    }

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // JUnit 4 runtime: alguna pieza de IntelliJ Platform lo referencia
    // (TestRule) aunque nuestros tests son JUnit 5.
    testRuntimeOnly("junit:junit:4.13.2")
    // kotlinx-coroutines-test tambien viene con IntelliJ Platform — no lo agregamos
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild").orElse("")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "9.1.0"
    }

    test {
        useJUnitPlatform()
    }
}
