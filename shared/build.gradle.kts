import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.androidx.room)
}

kotlin {
  android {
    namespace = "com.example.shared"
    compileSdk = 36
    minSdk = 24
    androidResources {
      enable = true
    }
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.jetbrains.compose.material.icons.extended)
      implementation(libs.jetbrains.compose.ui.backhandler)
      implementation(libs.jetbrains.lifecycle.viewmodel.compose)
      implementation(libs.jetbrains.lifecycle.runtime.compose)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.androidx.datastore.preferences.core)
      implementation(libs.okio)
      implementation(libs.androidx.room.runtime)
      implementation(libs.androidx.sqlite.bundled)
    }

    androidMain.dependencies {
      implementation(libs.androidx.core.ktx)
      implementation(libs.kotlinx.coroutines.android)
    }
  }
}

compose.resources {
  packageOfResClass = "com.example.shared.resources"
  publicResClass = true
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  add("kspAndroid", libs.androidx.room.compiler)
}
