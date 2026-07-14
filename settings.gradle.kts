pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "effect-app"
// Android modules are scaffolded in app/, core-model/, core-effects/, and core-pipeline/.
// The root build stays dependency-free so scaffold validation can run in minimal CI images.
