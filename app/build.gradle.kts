plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "br.com.taina.constantia"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.taina.constantia"
        minSdk = 26
        targetSdk = 37
        versionCode = 13
        versionName = "1.0.0-rc3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val stableKeystorePath = System.getenv("CONSTANTIA_KEYSTORE_PATH")
    val stableKeystorePassword = System.getenv("CONSTANTIA_KEYSTORE_PASSWORD")
    val stableKeyAlias = System.getenv("CONSTANTIA_KEY_ALIAS")
    val stableSigning = if (!stableKeystorePath.isNullOrBlank() && !stableKeystorePassword.isNullOrBlank() && !stableKeyAlias.isNullOrBlank()) {
        signingConfigs.create("constantiaStable") {
            storeFile = file(stableKeystorePath)
            storePassword = stableKeystorePassword
            keyAlias = stableKeyAlias
            keyPassword = stableKeystorePassword
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    } else null

    buildTypes {
        getByName("debug") {
            stableSigning?.let { signingConfig = it }
        }
        getByName("release") {
            isDebuggable = false
            stableSigning?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")

    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    testImplementation(kotlin("test"))
}
