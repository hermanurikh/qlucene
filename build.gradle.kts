import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.spring") version "1.4.21"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
}

group = "com.qbutton"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

apply(plugin = "org.jlleitschuh.gradle.ktlint")

dependencies {
    implementation("org.springframework:spring-context:5.3.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.methvin:directory-watcher:0.14.0")

    // for Apache Watchdog
    implementation("log4j:log4j:1.2.17")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs = listOf("-Xmx4096m")

    // the following tests check background events catching and use Thread.sleep for some significant number.
    // They are disabled by default, and can be turned on for some dev checks by -Dtest.profile=integration
    if (System.getProperty("test.profile") != "integration") {
        exclude("com/qbutton/qlucene/integration/background")
    }
}
