plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.dumper.android"
        minSdk = 21
        targetSdk = 32
        versionCode = 2
        versionName = "2.0.0"
        ndk {
            abiFilters.addAll(arrayOf("armeabi-v7a","arm64-v8a"))
        }
    }

    packagingOptions.jniLibs.useLegacyPackaging = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }
    ndkVersion = "23.1.7779620"
    namespace = "com.dumper.android"
}

dependencies {
    //UI
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    //Root 
    implementation("com.github.topjohnwu.libsu:core:3.2.1")
    implementation("com.github.topjohnwu.libsu:service:3.2.1")
}
