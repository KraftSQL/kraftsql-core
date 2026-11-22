allprojects {
    group = "rocks.frieler.kraftsql"
    version = "0.0.14-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

plugins {
    alias(libs.plugins.sonarqube)
}

tasks.register("versionFile") {
    group = "build"
    description = "Writes the current version to 'build/version.txt'."

    val versionTxt = layout.buildDirectory.get().file("version.txt").asFile
    val version = project.version.toString()
    doLast {
        versionTxt.apply {
            parentFile.mkdirs()
            writeText(version)
        }
    }
}

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "kraftsql")
        property("sonar.projectKey", "kraftsql_kraftsql-core")
        property("sonar.projectName", "KraftSQL Core")
        property("sonar.gradle.scanAll", "True")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/kover/report.xml")
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            when (val githubEvent = System.getenv("GITHUB_EVENT_NAME")) {
                "push" -> {
                    property("sonar.branch.name", System.getenv("GITHUB_REF_NAME"))
                }
                "pull_request" -> {
                    property("sonar.pullrequest.key", System.getenv("GITHUB_REF_NAME").removeSuffix("/merge"))
                    property("sonar.scm.revision", System.getenv("GITHUB_PR_HEAD_SHA"))
                }
                else -> logger.warn("unknown GITHUB_EVENT_NAME '$githubEvent'")
            }
        }
    }
}
project(":examples") {
    sonar.isSkipProject = true
}
tasks.sonar {
    for (subproject in subprojects.filterNot { it.sonar.isSkipProject }) {
        dependsOn("${subproject.name}:koverXmlReport")
    }
}

tasks.register<Exec>("fetchDefaultBranch") {
    group = "other"
    description = "Fetches the default branch of the current repository."
    commandLine("git fetch --no-tags origin +refs/heads/${System.getenv("GITHUB_REPO_DEFAULT_BRANCH_NAME")}:refs/remotes/origin/${System.getenv("GITHUB_REPO_DEFAULT_BRANCH_NAME")}".split(" "))
}.also { tasks.sonar.get().dependsOn(it) } // to enable comparison of the current branch with the default branch
