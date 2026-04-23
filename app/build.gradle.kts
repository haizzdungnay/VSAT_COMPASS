plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.v_sat_compass"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.v_sat_compass"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL_CLOUD", "\"https://vsat-compass-api.onrender.com/api/v1/\"")
            buildConfigField("String", "LOCAL_LAN_HOST", "\"192.168.1.240\"")
            buildConfigField("boolean", "USE_LOCAL_BACKEND", "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL_CLOUD", "\"https://vsat-compass-api.onrender.com/api/v1/\"")
            buildConfigField("String", "LOCAL_LAN_HOST", "\"192.168.1.240\"")
            buildConfigField("boolean", "USE_LOCAL_BACKEND", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testOptions {
        unitTests {
            // Android stubs return default values instead of throwing in JVM unit tests
            isReturnDefaultValues = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    // Image loading
    implementation(libs.glide)
    // Architecture (MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    // UI
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)
    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // CircleImageView
    implementation(libs.circleimageview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}