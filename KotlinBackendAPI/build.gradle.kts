plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.Neitirite"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-netty:3.1.2")
    implementation("io.ktor:ktor-server-websockets:3.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest{
        attributes["Main-Class"] = "com.Neitirite.MainAPIKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
}

kotlin {
    jvmToolchain(21)
}