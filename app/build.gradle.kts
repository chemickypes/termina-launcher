import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Carica le credenziali di firma da keystore.properties (radice del progetto,
// NON versionato). Se il file manca (es. macchina di un contributor o build
// F-Droid) la firma di release viene semplicemente saltata.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.hooloovoochimico.terminalauncher"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.hooloovoochimico.terminalauncher"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.1"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    // Due distribuzioni: `foss` (F-Droid, accesso completo al filesystem via sudo)
    // e `play` (Google Play, confinata alla cartella dell'app — niente
    // MANAGE_EXTERNAL_STORAGE, che Play non concede a un launcher).
    flavorDimensions += "dist"
    productFlavors {
        create("foss") {
            dimension = "dist"
            buildConfigField("boolean", "FULL_ACCESS", "true")
        }
        create("play") {
            dimension = "dist"
            buildConfigField("boolean", "FULL_ACCESS", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // firma solo se keystore.properties è presente (altrimenti build non firmata)
            signingConfig = signingConfigs.findByName("release")
            // simboli di debug nativi (per le .so transitive, es. androidx.graphics.path):
            // vengono inclusi nell'AAB e risolvono l'avviso "native debug symbols" di Play.
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    // I test locali girano su JVM con Robolectric: servono le risorse Android
    // (getString ecc.) impacchettate e il framework Android emulato.
    testOptions {
      unitTests {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.kotlinx.coroutines.android)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Robolectric (Context Android su JVM)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  // Test UI Compose che girano su JVM sotto Robolectric
  testImplementation(composeBom)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.compose.ui.test.manifest)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
