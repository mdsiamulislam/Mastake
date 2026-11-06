plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "me.binarylife.mastaka"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.binarylife.mastaka"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Credentials & Google ID (Credential Manager for Firebase Auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // UI Components
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Firebase BoM (সঠিক ভার্সন শুধু একবার থাকবে)
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    // Firebase Products (BoM version কন্ট্রোল করবে তাই আলাদা ভার্সন লাগবে না)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Gemini SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}