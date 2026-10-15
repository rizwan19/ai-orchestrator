import com.github.gradle.node.npm.task.NpmTask

// Gradle entry point for the Next.js app. A pinned Node.js is downloaded into
// .gradle/nodejs, so the system `node` version does not matter.
// Usage: ./gradlew dev [-Pport=3000]   |   ./gradlew start   |   ./gradlew test

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
}
val nextBuild = npmScript("nextBuild", "Creates a production build.", "run", "build")
npmScript("start", "Serves the production build.", "run", "start", "--", "--port").configure {
    dependsOn(nextBuild)
    args.add(port)
}
npmScript("test", "Runs the Vitest suite.", "test")
npmScript("lint", "Runs ESLint.", "run", "lint")
