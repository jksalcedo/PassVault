import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.autonomousapps.dependency-analysis")
    alias(libs.plugins.aboutLibraries)
}

configure<ApplicationExtension> {
    namespace = "com.jksalcedo.passvault"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jksalcedo.passvault"
        minSdk = 28
        targetSdk = 37
        versionCode = 43
        versionName = "2.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.pickFirsts.add("META-INF/com/android/build/gradle/app-metadata.properties")
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }

        jniLibs {
            useLegacyPackaging = false
        }
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
            val abi = output.filters.find {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"

            val apkName = "${project}-v${version}-b${code}-${abi}-${buildType}.apk"

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
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.androidx.documentfile)
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
    implementation(libs.kotlinx.serialization.csv)

    // https://mvnrepository.com/artifact/androidx.preference/preference
    implementation(libs.androidx.preference)

    // WorkManager Testing
    androidTestImplementation(libs.androidx.work.testing)
    testImplementation(libs.androidx.work.testing)

    // Mocking library
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk.android)

    implementation(libs.kotpass)

    // Argon2
    implementation(libs.argon2kt)

    implementation(libs.aboutlibraries)

}
