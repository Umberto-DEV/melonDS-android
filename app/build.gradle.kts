import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Opt-in native sanitizer support (HWASan on device, or ASan/UBSan/etc): pass -PnativeSanitize=<value>
// (e.g. hwaddress) to have CMake configure the NDK's sanitizer for this build only. Off by default.
// See tools/README.md. Hoisted to file scope so both the debug buildType block and the androidComponents
// block below (needed to scope packaging options to just this variant) can see it.
val nativeSanitize = project.findProperty("nativeSanitize") as String?

android {
    signingConfigs {
        create("release") {
            val props = Properties().apply {
                val file = rootProject.file("local.properties")
                if (file.exists()) load(file.inputStream())
            }
            (props["MELONDS_KEYSTORE"] as String?)?.let { storeFile = file(it) }
            storePassword = props["MELONDS_KEYSTORE_PASSWORD"] as String? ?: ""
            keyAlias = props["MELONDS_KEY_ALIAS"] as String? ?: ""
            keyPassword = props["MELONDS_KEY_PASSWORD"] as String? ?: ""
        }
    }

    namespace = "me.magnum.melonds"
    compileSdk = AppConfig.compileSdkVersion
    ndkVersion = AppConfig.ndkVersion
    defaultConfig {
        applicationId = "me.magnum.melonds"
        minSdk = AppConfig.minSdkVersion
        targetSdk = AppConfig.targetSdkVersion
        versionCode = AppConfig.versionCode
        versionName = AppConfig.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -Wno-write-strings")
            }
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Android 16+ shared-memory shadows use this JDK bridge on Java 21.
            it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"

            if (nativeSanitize != null) {
                externalNativeBuild {
                    cmake {
                        arguments("-DANDROID_SANITIZE=$nativeSanitize", "-DANDROID_STL=c++_shared")
                    }
                }
                // HWASan (and other sanitizer) builds need a wrap.sh that sets LD_HWASAN=1 to
                // run on a device/emulator whose OS image isn't itself built with the sanitizer
                // (see https://developer.android.com/ndk/guides/hwasan and .../wrap-script).
                // Android Studio only packages .so files from lib/, so wrap.sh has to live
                // under resources/lib/<abi>/ instead, and useLegacyPackaging must be turned on
                // for that directory layout to be packaged as-is (done below, scoped to just this
                // variant via androidComponents: "packaging" here resolves to the top-level android.packaging
                // extension, which would apply it to release too).
                sourceSets.getByName("debug").resources.srcDir(rootProject.file("tools/hwasan-resources"))
            }
        }

    }

    flavorDimensions += listOf("version", "build")
    productFlavors {
        create("playStore") {
            dimension = "version"
            versionNameSuffix = " PS"
        }
        create("gitHub") {
            dimension = "version"
            isDefault = true
            versionNameSuffix = " GH"
            ndk {
                // Add 32 bit support only on GitHub releases
                abiFilters.add("armeabi-v7a")
            }
        }

        create("prod") {
            dimension = "build"
            isDefault = true
        }
        create("nightly") {
            dimension = "build"
            applicationIdSuffix = ".nightly"
            versionNameSuffix = " (NIGHTLY)"
        }
    }
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.directories += "$projectDir/schemas"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Package the optional native sanitizer runtime only for debug builds.
androidComponents {
    if (nativeSanitize != null) {
        onVariants(selector().withBuildType("debug")) { variant ->
            variant.packaging.jniLibs.useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.masterswitch)
    implementation(projects.rcheevosApi)
    implementation(projects.common)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.rxjava)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.window)
    implementation(libs.androidx.work)
    implementation(libs.android.material)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.markdown)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coil)
    implementation(libs.gson)
    implementation(libs.hilt)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.coroutines.rx)
    implementation(libs.picasso)
    implementation(libs.commons.compress)
    implementation(libs.xz)

    "gitHubImplementation"(libs.retrofit)
    "gitHubImplementation"(libs.retrofit.converter.kotlinx)

    ksp(libs.hilt.compiler)
    ksp(libs.hilt.compiler.android)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinxCoroutines.get()}")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
