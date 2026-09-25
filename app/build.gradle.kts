import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.net.HttpURLConnection
import java.net.URL

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.snapbrand.v8jzk"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("PRINTIFY_API_KEY")
  ignoreList.add("PRINTIFY_API_TOKEN")
  ignoreList.add("PRINTIFY_WEBHOOK_SECRET")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("verifyPrintifyAuth") {
  group = "verification"
  description = "Safely tests Printify authentication using server-side PRINTIFY_API_KEY without exposing credentials."
  val dotEnvFile = rootProject.layout.projectDirectory.file(".env").asFile

  doLast {
    var apiKey = System.getenv("PRINTIFY_API_KEY") ?: System.getenv("PRINTIFY_API_TOKEN")
    if (apiKey.isNullOrBlank() && dotEnvFile.exists()) {
      dotEnvFile.forEachLine { line ->
        val trimmed = line.trim()
        if (!trimmed.startsWith("#") && (trimmed.startsWith("PRINTIFY_API_KEY=") || trimmed.startsWith("PRINTIFY_API_TOKEN="))) {
          val v = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
          if (v.isNotBlank() && !v.startsWith("MY_") && !v.equals("placeholder", ignoreCase = true)) {
            apiKey = v
          }
        }
      }
    }

    if (apiKey.isNullOrBlank()) {
      println("PRINTIFY_AUTHENTICATION: FAILED")
      println("Reason: PRINTIFY_API_KEY is not configured in server environment or .env file.")
      return@doLast
    }

    try {
      @Suppress("DEPRECATION")
      val url = URL("https://api.printify.com/v1/shops.json")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("User-Agent", "SnapBrand/1.0 (Auth Verification)")
        setRequestProperty("Accept", "application/json")
      }
      val code = conn.responseCode
      if (code in 200..299) {
        val stream = conn.inputStream.bufferedReader().use { it.readText() }
        val titles = Regex(""""title"\s*:\s*"([^"]+)"""").findAll(stream).map { it.groupValues[1] }.toList()
        println("PRINTIFY_AUTHENTICATION: SUCCESS")
        val count = titles.size
        val shopList = if (titles.isNotEmpty()) titles.joinToString(", ") else "None"
        println("Authorized Printify account connected. Found $count shop(s): $shopList")
      } else {
        println("PRINTIFY_AUTHENTICATION: FAILED")
        println("HTTP status: $code")
      }
      conn.disconnect()
    } catch (e: Exception) {
      println("PRINTIFY_AUTHENTICATION: FAILED")
      println("Network or connection error: ${e.javaClass.simpleName}")
    }
  }
}



