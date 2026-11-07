plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "it.driveos.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "it.driveos.launcher"
        // La Junsun FF_866X monta Android 10 (API 29): e' il minimo supportato.
        minSdk = 29
        targetSdk = 34
        versionCode = 140
        versionName = "1.1.5"
        // solo processori ARM (la radio e' ARM): via le librerie x86 di Vosk/JNA, APK molto piu' leggero
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    signingConfigs {
        // Chiave di DEBUG fissa (non segreta): permette di aggiornare l'app sulla radio
        // con ogni nuova build senza disinstallarla. Non usarla per pubblicare.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Nessuna chiave nel repository: la firma release si configura a parte (vedi README).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        resources.excludes += "/META-INF/LICENSE*"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("dev.mobile:dadb:1.2.9") { // permessi via adb locale (SelfSetup)
        // dadb si porta dietro per errore le librerie di test JUnit: non servono nell'app
        exclude(group = "org.junit.platform"); exclude(group = "org.junit.jupiter")
        exclude(group = "org.opentest4j"); exclude(group = "org.apiguardian"); exclude(group = "junit")
    }
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    // Anteprime delle schermate (solo test, workflow "Anteprime schermate")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
