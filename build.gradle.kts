plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("kapt") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

group = "com.bluedragonmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    kapt("com.velocitypowered:velocity-api:3.0.1")

    compileOnly("dev.simplix:protocolize-api:2.2.4")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.bluedragonmc"
            artifactId = "Jukebox"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}