plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android { namespace = "org.effectapp.pipeline"; compileSdk = 36; defaultConfig { minSdk = 24 } }
dependencies { implementation(project(":core-model")); implementation(project(":core-effects")); implementation(libs.androidx.media3.transformer); implementation(libs.androidx.media3.effect); implementation(libs.kotlinx.coroutines.android) }
