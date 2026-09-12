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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"

            // Opt-in native sanitizer support (HWASan on device, or ASan/UBSan/etc): pass
            // -PnativeSanitize=<value> (e.g. hwaddress) to have CMake configure the NDK's
            // sanitizer for this build only. Off by default. See tools/README.md.
            val nativeSanitize = project.findProperty("nativeSanitize") as String?
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
                // for that directory layout to be packaged as-is.
                sourceSets.getByName("debug").resources.srcDir(rootProject.file("tools/hwasan-resources"))
                packaging.jniLibs.useLegacyPackaging = true
            }
        }
        // Performance measurement variant. The "debug" type builds the native code with no
        // -O flag at all (clang then defaults to -O0), which makes any CPU profile taken from
        // it meaningless, and "release" cannot be built here because it needs the signing
        // material from local.properties. This one is debug-signed and installable like debug,
        // but compiles the native code as RelWithDebInfo: -O2 -g -DNDEBUG, optimised AND with
        // symbols, which is what simpleperf needs to resolve names.
        // It exists so that "debug" and "release" can stay exactly as they are.
        create("profiling") {
            initWith(getByName("debug"))
            // Own application id, so it installs next to the .dev build instead of replacing it.
            applicationIdSuffix = ".perf"
            versionNameSuffix = " (PROFILING)"
            signingConfig = signingConfigs.getByName("debug")
            // NOT debuggable: a debuggable app runs under ART with optimisations
            // disabled, which is a large, constant tax on everything outside the
            // native core. Measured on device: the stable 2.0.1 is compiled
            // speed-profile while this build was run-from-apk, i.e. not compiled
            // at all -- which made every comparison between the two meaningless.
            isDebuggable = false
            isMinifyEnabled = false
            // The library modules only declare debug/release; without this, dependency
            // resolution for this build type fails.
            matchingFallbacks += listOf("debug")
            externalNativeBuild {
                cmake {
                    arguments("-DCMAKE_BUILD_TYPE=RelWithDebInfo")
                }
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

// Ship the native libraries unstripped in the profiling APK only, so a profiler reads the
// symbol names straight off the device. Scoped to this build type through the variant API
// on purpose: the android.packaging block would apply to debug and release as well.
androidComponents {
    onVariants(selector().withBuildType("profiling")) { variant ->
        variant.packaging.jniLibs.keepDebugSymbols.add("**/*.so")
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

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}