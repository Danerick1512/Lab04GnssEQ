plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lab.lab04eq"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lab.lab04eq"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    // ── Dependencias Base del Sistema ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Jetpack Compose BOM & Material 3 (Material Theme Builder) ──
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Iconos extendidos oficiales de Material Design
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    // Navegación nativa en Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ── Room Database (Persistencia Local) ──
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── Jetpack DataStore (Preferencia de modo oscuro/sesión) ──
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Arquitectura de Ciclo de Vida y ViewModels ──
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // ── Google Location Services ──
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // ── Coil (Carga optimizada de imágenes) ──
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Accompanist (Gestor de Permisos) ──
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // ── Entorno de Pruebas ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}