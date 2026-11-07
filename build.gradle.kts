
plugins {
    id("java")
    id("maven-publish")
}

group = "de.LundLucenany9"
version = "1.0.2"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()

}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.lundlucenany9.ees.main.Main"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}