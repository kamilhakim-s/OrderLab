import io.gatling.gradle.GatlingPluginExtension

plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    id("jacoco")
    id("info.solidsoft.pitest") version "1.15.0"
    id("org.owasp.dependencycheck") version "9.2.0"
    id("org.sonarqube") version "5.0.0.4638"
    id("io.gatling.gradle") version "3.11.3"
    `maven-publish`
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

// ── Source sets ──────────────────────────────────────────────────────────────
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

// ── Repositories ─────────────────────────────────────────────────────────────
repositories {
    mavenCentral()
}

// ── Dependency versions ───────────────────────────────────────────────────────
val testcontainersVersion = "1.20.4"

// ── Dependencies ─────────────────────────────────────────────────────────────
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("com.h2database:h2")

    integrationTestImplementation("org.testcontainers:junit-jupiter")
    integrationTestImplementation("org.testcontainers:postgresql")
    integrationTestImplementation("org.testcontainers:rabbitmq")
    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.awaitility:awaitility:4.2.1")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

// ── integrationTest task ──────────────────────────────────────────────────────
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")
    useJUnitPlatform()
    val osName = System.getProperty("os.name").lowercase()
    val defaultDockerHost = when {
        osName.contains("mac") -> "unix:///Users/${System.getProperty("user.name")}/.docker/run/docker.sock"
        osName.contains("linux") -> "unix:///var/run/docker.sock"
        osName.contains("windows") -> "npipe:////./pipe/docker_engine"
        else -> "unix:///var/run/docker.sock"
    }
    // Docker Desktop 4.60+ requires API version >= 1.44 (set in ~/.docker-java.properties)
    val dockerHost = System.getenv("DOCKER_HOST") ?: defaultDockerHost
    environment("DOCKER_HOST", dockerHost)
    environment("DOCKER_API_VERSION", "1.44")
    if (dockerHost.startsWith("unix://")) {
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", dockerHost.removePrefix("unix://"))
    }
    // Disable Ryuk resource reaper (avoids Docker Hub pull for testcontainers/ryuk)
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

// ── JaCoCo ───────────────────────────────────────────────────────────────────
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ── Pitest ───────────────────────────────────────────────────────────────────
pitest {
    junit5PluginVersion = "1.2.1"
    targetClasses = setOf("com.example.orderservice.*")
    excludedClasses = setOf(
        "com.example.orderservice.OrderServiceApplication",
        "com.example.orderservice.config.*"
    )
    mutationThreshold = 70
    coverageThreshold = 80
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
}

// ── OWASP ────────────────────────────────────────────────────────────────────
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "owasp-suppressions.xml"
}

// ── Sonar ────────────────────────────────────────────────────────────────────
sonarqube {
    properties {
        property("sonar.projectKey", "order-service")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
    }
}

// ── Gatling ──────────────────────────────────────────────────────────────────
configure<GatlingPluginExtension> {
    // Gatling source set is auto-configured at src/gatling/scala
}

// ── Publishing ───────────────────────────────────────────────────────────────
publishing {
    publications {
        create<MavenPublication>("bootJar") {
            artifact(tasks.bootJar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "owner/order-service"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
