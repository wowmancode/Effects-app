plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.serialization) }

android { namespace = "org.effectapp.app"; compileSdk = 36
    defaultConfig { applicationId = "org.effectapp.app"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core-model")); implementation(project(":core-effects")); implementation(project(":core-pipeline"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.activity.compose); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.ui.tooling.preview); implementation(libs.androidx.navigation.compose); implementation(libs.androidx.media3.exoplayer); implementation(libs.androidx.media3.ui); implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
