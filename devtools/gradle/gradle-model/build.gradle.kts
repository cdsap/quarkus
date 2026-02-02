plugins {
    id("io.quarkus.devtools.java-library")
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    gradleApi()
}

group = "io.quarkus"
//version = "0129"
java {
    withSourcesJar()
    withJavadocJar()
}

// to generate reproducible jars
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "quarkus-gradle-model"
        from(components["java"])
    }
}
