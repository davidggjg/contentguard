plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.contentguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.contentguard"
        minSdk = 28  // Android 9 – נדרש ל-Private DNS ו-EncryptedSharedPreferences
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true   // כווץ קוד ב-release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX בסיסי
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // הצפנת SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines – לפעולות רשת ברקע (הורדת blocklist)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // TODO לגרסה הבאה: OkHttp להורדת blocklist
    // implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
