import com.github.gradle.node.npm.task.NpmTask

// Gradle entry point for the Next.js app. A pinned Node.js is downloaded into
// .gradle/nodejs, so the system `node` version does not matter.
// Usage: ./gradlew devAll (backend + frontend)  |  dev [-Pport=3000]  |  start  |  test

plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    // Keep in sync with "engines" in package.json and .nvmrc.
    version = "22.21.1"
    download = true
}

val port = providers.gradleProperty("port").orElse("3000")

tasks.npmInstall {
    // Reproducible install from the lockfile.
    npmCommand = listOf("ci")
}

fun npmScript(name: String, description: String, vararg args: String) =
    tasks.register<NpmTask>(name) {
        group = "frontend"
        this.description = description
        dependsOn(tasks.npmInstall)
        this.args = args.toList()
    }

npmScript("dev", "Starts the Next.js dev server.", "run", "dev", "--", "--port").configure {
    args.add(port)
    doFirst {
        execOverrides.set {
            standardOutput = LineForwarder("frontend")
            errorOutput = LineForwarder("frontend")
        }
    }
}
val nextBuild = npmScript("nextBuild", "Creates a production build.", "run", "build")
npmScript("start", "Serves the production build.", "run", "start", "--", "--port").configure {
    dependsOn(nextBuild)
    args.add(port)
}
npmScript("test", "Runs the Vitest suite.", "test")
npmScript("lint", "Runs ESLint.", "run", "lint")

// Starts the backend (Postgres via Docker Compose + Spring Boot on :8080) and the Next.js dev server together.
// They run concurrently because they live in different projects and org.gradle.parallel=true.
// Ctrl+C stops both.
tasks.register("devAll") {
    group = "application"
    description = "Starts the Spring Boot backend and the Next.js dev server together."
    dependsOn(":backend:bootRun", "dev")
}
