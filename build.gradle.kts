plugins {
    kotlin("jvm") version "1.7.20"
}

repositories {
    mavenCentral()

    maven("https://jitpack.io")
}

dependencies {
    implementation(project("Doom"))

    implementation("com.github.Minestom:Minestom:9dab3183e5")
}