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
    implementation("io.minio:minio:8.5.17")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    manifest{
        attributes["Main-Class"] = "com.Neitirite.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/MANIFEST.MF")
    }
}
kotlin {
    jvmToolchain(21)
}