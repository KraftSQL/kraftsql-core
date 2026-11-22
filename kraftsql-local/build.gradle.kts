project.description = "KraftSQL Local Engine"

plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    `java-library`
    idea
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.kover)
    id("kraftsql-publishing")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(project(":kraftsql"))

    testImplementation(libs.junit.api)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
