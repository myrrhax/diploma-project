plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.github.myrrhax"
version = "0.0.1-SNAPSHOT"

val libVersions = mapOf(
    "liquibase" to "5.0.1",
    "jjwt" to "0.13.0",
    "mapstruct" to "1.6.3"
)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.liquibase:liquibase-core:${libVersions["liquibase"]}")
    implementation("io.jsonwebtoken:jjwt-api:${libVersions["jjwt"]}")
    implementation("org.mapstruct:mapstruct:${libVersions["mapstruct"]}")

    compileOnly("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${libVersions["jjwt"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${libVersions["jjwt"]}")

    annotationProcessor("org.mapstruct:mapstruct-processor:${libVersions["mapstruct"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
