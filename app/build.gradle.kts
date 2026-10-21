plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val rumoSigningStore = providers.environmentVariable("RUMO_SIGNING_STORE").orNull
val rumoSigningStorePassword = providers.environmentVariable("RUMO_SIGNING_STORE_PASSWORD").orNull
val rumoSigningAlias = providers.environmentVariable("RUMO_SIGNING_KEY_ALIAS").orNull
val rumoSigningKeyPassword = providers.environmentVariable("RUMO_SIGNING_KEY_PASSWORD").orNull
val hasRumoReleaseSigning = listOf(rumoSigningStore, rumoSigningStorePassword, rumoSigningAlias, rumoSigningKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.hirden.flomofloat"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.hirden.flomofloat"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.4.3"
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        if (hasRumoReleaseSigning) {
            create("maintainer") {
                storeFile = file(requireNotNull(rumoSigningStore))
                storePassword = rumoSigningStorePassword
                keyAlias = rumoSigningAlias
                keyPassword = rumoSigningKeyPassword
            }
        }
    }
    buildTypes {
        // Debug builds use the developer's standard local Android debug key.
        release {
            isMinifyEnabled = false
            if (hasRumoReleaseSigning) signingConfig = signingConfigs.getByName("maintainer")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(project(":floatingx-system"))
    implementation("androidx.core:core:1.13.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
