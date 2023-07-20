import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    kotlin("jvm") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

group = "github.rudashyverse"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperVersion: String by project

dependencies {
    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    compileOnly("io.github.distractic:bukkit-api:1.0.0")

    testImplementation(kotlin("test"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")

    }
}