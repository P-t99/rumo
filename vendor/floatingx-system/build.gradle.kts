plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.petterp.floatingx.system"
    compileSdk = 36
    defaultConfig { minSdk = 21 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":floatingx-core"))
    implementation("androidx.core:core:1.13.1")
    api("androidx.annotation:annotation:1.9.1")
}
