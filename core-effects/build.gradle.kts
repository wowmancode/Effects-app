plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android { namespace = "org.effectapp.effects"; compileSdk = 36; defaultConfig { minSdk = 24 } }
dependencies { implementation(project(":core-model")); implementation(libs.androidx.media3.effect); implementation(libs.androidx.media3.transformer); testImplementation(libs.junit) }
