import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.6.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "org.enricher"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// NOTE: We cannot use "compileOnly" or "shadow" configurations since then we could not run code
// in the IDE or with "gradle run". We also cannot exclude transitive dependencies from the
// shadowJar yet (see https://github.com/johnrengelman/shadow/issues/159).
// -> Explicitly define the // libraries we want to be included in the "flinkShadowJar" configuration!

val flinkShadowJar: Configuration by configurations.creating

// Compile-time dependencies that should NOT be part of the
// shadow jar and are provided in the lib folder of Flink,
// must use "implementation" configuration
// --------------------------------------------------------------
// Dependencies that should be part of the shadow jar, e.g.
// connectors, must be in the "flinkShadowJar" configuration!
configure<SourceSetContainer> {
    named<SourceSet>("main") {
        compileClasspath += flinkShadowJar
        runtimeClasspath += flinkShadowJar
    }
    named<SourceSet>("test") {
        compileClasspath += flinkShadowJar
        runtimeClasspath += flinkShadowJar
    }
}
application {
    mainClass.set("org.enricher.EnrichmentJob")
}

configurations {
    // provided by Flink
    flinkShadowJar.exclude("org.apache.flink", "force-shading")
    flinkShadowJar.exclude("com.google.code.findbugs", "jsr305")
    flinkShadowJar.exclude("org.slf4j")
    flinkShadowJar.exclude("org.apache.logging.log4j")
}

val flinkVersion = "1.20.0"
val flinkConnectorVersion = "3.2.0-1.18"
val avroVersion = "1.12.0"

dependencies {
    implementation("org.apache.flink:flink-java:${flinkVersion}")
    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-avro:${flinkVersion}")
    implementation("org.apache.avro:avro:${avroVersion}")

    flinkShadowJar("org.apache.flink:flink-connector-kafka:${flinkConnectorVersion}")
    flinkShadowJar("org.apache.flink:flink-avro:${flinkVersion}")
    flinkShadowJar("org.apache.avro:avro:${avroVersion}")

    testImplementation("org.apache.flink:flink-test-utils:${flinkVersion}")
    testImplementation("org.apache.flink:flink-runtime:${flinkVersion}")
    testImplementation("org.apache.flink:flink-streaming-java:${flinkVersion}:tests")
    testImplementation("org.assertj:assertj-core:3.25.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
}

tasks {
    named<ShadowJar>("shadowJar") {
        configurations = listOf(flinkShadowJar)
    }

    named<Test>("test") {
        useJUnitPlatform()
    }
}

