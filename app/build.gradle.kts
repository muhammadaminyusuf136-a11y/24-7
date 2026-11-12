import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "uz.reja24.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "uz.reja24.app"
        minSdk = 26          // Android 8.0 va undan yuqori
        targetSdk = 36       // Eng so'nggi Android
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    // Play Market uchun imzo. Kalit fayli va parollar KODDA saqlanmaydi —
    // ular GitHub Secrets yoki kompyuterdagi muhit o'zgaruvchilaridan olinadi.
    val keystorePath: String? = System.getenv("REJA_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("REJA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("REJA_KEY_ALIAS")
                keyPassword = System.getenv("REJA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            // R8: kodni kichraytiradi va chalkashtiradi (APK hajmi kichik bo'ladi)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // ".dev" — telefonda avval boshqa imzo bilan o'rnatilgan ".debug" versiya qoldig'i bilan to'qnashmasligi uchun
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Unit testlarda Android klasslari chaqirilsa yiqilmasin (standart qiymat qaytaradi)
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // Tarjimalar to'liq bo'lishi shart (4 til)
        error += "MissingTranslation"
        abortOnError = true
        checkReleaseBuilds = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Room ma'lumot bazasi sxemalari shu papkaga saqlanadi (migratsiyalar uchun kerak)
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // AndroidX asosiy
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)          // til almashtirish (setApplicationLocales) uchun
    implementation(libs.androidx.activity.compose)

    // Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Lifecycle / ViewModel / Navigatsiya
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Bosh ekran vidjeti (Jetpack Glance)
    implementation(libs.androidx.glance.appwidget)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Unit testlar
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    // Qurilmadagi testlar
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
}
