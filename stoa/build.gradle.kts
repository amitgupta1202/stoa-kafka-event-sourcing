import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.61")
    }
}

plugins {
    application
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.61"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.61")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    implementation("org.apache.kafka:kafka-streams:2.3.0")
    implementation("io.arrow-kt:arrow-core:0.10.3")
    implementation("io.arrow-kt:arrow-core-data:0.10.3")
    implementation("io.arrow-kt:arrow-fx:0.10.3")
    implementation("org.apache.commons:commons-pool2:2.0")
    implementation("redis.clients:jedis:2.8.1")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("org.jooq:jooq:3.12.3")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("org.postgresql:postgresql:42.2.9")
    implementation("org.http4k:http4k-core:3.215.0")
    implementation("org.http4k:http4k-server-netty:3.215.0")
    implementation("org.http4k:http4k-client-okhttp:3.215.0")

    testImplementation("org.assertj:assertj-core:3.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testImplementation("org.awaitility:awaitility-kotlin:4.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClassName = "com.annasystems.stoa.AppKt"
}


