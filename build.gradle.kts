import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

group = "no.nav.syfo"
version = "1.0.0"

val jacksonVersion = "2.17.2"
val kluentVersion = "1.73"
val logbackVersion = "1.5.6"
val ktorVersion = "3.0.1"
val logstashEncoderVersion = "8.0"
val prometheusVersion = "0.16.0"
val mockkVersion = "1.13.12"
val nimbusdsVersion = "9.40"
val hikariVersion = "5.1.0"
val flywayVersion = "10.17.1"
val postgresVersion = "42.7.3"
val testContainerVersion = "1.20.1"
val kotlinVersion = "2.0.10"
val kotestVersion = "5.9.1"
val googlePostgresVersion = "1.19.1"
val googleOauthVersion = "1.36.0"
val ktfmtVersion = "0.44"
val kafkaVersion = "3.8.0"

//Due to vulnerabilities
val nettycommonVersion = "4.1.115.Final"
val commonsCompressVersion = "1.27.0"


plugins {
    id("com.diffplug.spotless") version "6.25.0"
    kotlin("jvm") version "2.0.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:$nettycommonVersion") {
            because("Due to vulnerabilities in io.ktor:ktor-server-netty")
        }
    }
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.google.cloud.sql:postgres-socket-factory:$googlePostgresVersion")
    implementation("com.google.oauth-client:google-oauth-client:$googleOauthVersion")
    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")


    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    constraints {
        testImplementation("org.apache.commons:commons-compress:$commonsCompressVersion") {
            because("overrides vulnerable dependency from org.testcontainers:postgresql")
        }
    }
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}



tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
    }
    create("printVersion") {
        println(project.version)
    }

    withType<ShadowJar> {
mergeServiceFiles {
     setPath("META-INF/services/org.flywaydb.core.extensibility.Plugin")
 }
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}


