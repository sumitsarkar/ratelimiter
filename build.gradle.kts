plugins {
    kotlin("jvm") version "1.8.0"
}

group = "me.sumit.ratelimiter"
version = "0.2.0"

val junitVersion = "5.3.2"

repositories {
    mavenCentral()
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("io.github.microutils:kotlin-logging:1.6.22")

    implementation("org.slf4j:slf4j-simple:1.7.25")

    testImplementation(kotlin("test"))
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
