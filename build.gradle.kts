import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    embeddedKotlin("jvm")
    embeddedKotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `java-library`
}

group = "com.github.rushyverse"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    val paperVersion = "1.19-R0.1-SNAPSHOT"
    val rushyApiVersion = "1.0.0"
    val commandApiVersion = "9.0.3"

    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    compileOnly("com.github.Rushyverse:api:$rushyApiVersion")

    // CommandAPI framework
    compileOnly("dev.jorel:commandapi-bukkit-core:$commandApiVersion")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:$commandApiVersion")

    testImplementation(kotlin("test"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
    }
}