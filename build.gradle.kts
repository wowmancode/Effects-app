plugins { base }

val moduleNames = listOf("app", "core-model", "core-effects", "core-pipeline")

tasks.register("lint") {
    group = "verification"
    description = "Placeholder lint task for environments without the Android SDK."
}

tasks.register("test") {
    group = "verification"
    description = "Placeholder aggregate test task for the scaffold."
}

tasks.named("check") { dependsOn("lint", "test") }

tasks.register("assembleDebug") { dependsOn("assemble") }
tasks.register("assembleRelease") { dependsOn("assemble") }

moduleNames.forEach { moduleName ->
    tasks.register("validate${moduleName.replaceFirstChar { it.uppercase() }}Scaffold") {
        inputs.dir(moduleName)
        doLast { check(file(moduleName).isDirectory) { "Missing $moduleName" } }
    }
}
