import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
}

group = "de.LundLucenany9"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()

}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}