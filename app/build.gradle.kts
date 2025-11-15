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
        versionCode = 6
        versionName = "0.5.0"

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

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputImpl = output as com.android.build.api.variant.impl.VariantOutputImpl

            val project = "PassVault"
            val version = output.versionName.get()
            val code = output.versionCode.get()
            val buildType = variant.buildType

            val apkName = "${project}-v${version}-b${code}-${buildType}.apk"

            outputImpl.outputFileName.set(apkName)
        }
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
    // for csv
    // https://mvnrepository.com/artifact/de.brudaswen.kotlinx.serialization/kotlinx-serialization-csv
    implementation("de.brudaswen.kotlinx.serialization:kotlinx-serialization-csv:3.1.0")

    // https://mvnrepository.com/artifact/androidx.preference/preference
    implementation(libs.androidx.preference)
}
