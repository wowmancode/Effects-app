import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android { namespace = "org.effectapp.effects"; compileSdk = 35; defaultConfig { minSdk = 24 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies { implementation(project(":core-model")); implementation(libs.androidx.media3.effect); implementation(libs.androidx.media3.transformer); testImplementation(libs.junit) }
