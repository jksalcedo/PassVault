import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "2.2.21"

}

android {
    namespace = "com.jksalcedo.passvault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jksalcedo.passvault"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4-alpha"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Biometrics for fingerprint/face unlock
    implementation(libs.androidx.biometric)

    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.espresso.intents)

    implementation(libs.kotlinx.serialization.json)
    // https://mvnrepository.com/artifact/androidx.preference/preference
    implementation(libs.androidx.preference)
}