import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.40"
}

group = "me.sumit.ratelimiter"
version = "0.1.0"

val junitVersion = "5.3.2"

repositories {
    mavenCentral()
    jcenter()
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core: 1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.2.2")

    implementation("io.github.microutils:kotlin-logging:1.6.22")

    implementation("org.slf4j:slf4j-simple:1.7.25")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}