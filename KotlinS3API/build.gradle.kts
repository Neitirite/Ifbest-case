plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.minio:minio:8.5.17")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}