plugins {
    kotlin("jvm") version "1.9.20"
}

group = "com.ecommerce.analyzer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // CSV Processing
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
    
    // Date/Time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    
    // Coroutines for async processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation(kotlin("test"))
}



tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"  // Use 21 which is supported by Kotlin
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    targetCompatibility = "21"
    sourceCompatibility = "21"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.ecommerce.analyzer.MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
