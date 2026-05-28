plugins {
    java
    application
}

group = "com.animetracker"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
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
    implementation(platform("org.springframework:spring-framework-bom:7.0.7"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.2"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-orm")
    implementation("org.springframework:spring-webmvc")
    implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.6")
    implementation("org.springframework.data:spring-data-jpa:4.0.5")
    implementation("org.hibernate.orm:hibernate-core:7.2.12.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.springframework.kafka:spring-kafka:4.0.5")
    implementation("org.telegram:telegrambots-longpolling:7.11.0")
    implementation("org.telegram:telegrambots-client:7.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.32")

    implementation(platform("org.springframework.modulith:spring-modulith-bom:2.0.6"))
    compileOnly("org.springframework.modulith:spring-modulith-core:2.0.6")
    testImplementation(platform("org.springframework.modulith:spring-modulith-bom:2.0.6"))
    testImplementation("org.springframework.modulith:spring-modulith-core")
    testImplementation("org.springframework.boot:spring-boot:4.0.6")

    runtimeOnly("org.postgresql:postgresql:42.7.10")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.springframework.kafka:spring-kafka-test:4.0.5")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
}

application {
    mainClass = "com.animetracker.AnimeTrackerApplication"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
