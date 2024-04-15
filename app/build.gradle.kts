plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id ("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.xperiencelabs.arimage"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xperiencelabs.arimage"
        minSdk = 24
        targetSdk = 34
        versionCode = 9
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")

    implementation ("com.gorisse.thomas.sceneform:sceneform:1.23.0")
    implementation ("com.google.android.filament:filamat-android:1.21.1")
    implementation ("com.github.fondesa:kpermissions:3.4.0")

    //hilt dependencies
    implementation ("com.google.dagger:hilt-android:2.48")
    ksp ("com.google.dagger:hilt-android-compiler:2.48")
    ksp ("androidx.hilt:hilt-compiler:1.0.0")

    //room
    implementation ("androidx.room:room-runtime:2.5.2")
    ksp ("androidx.room:room-compiler:2.5.2")
    implementation ("androidx.room:room-ktx:2.5.2")

    //sdp
    implementation ("com.intuit.sdp:sdp-android:1.1.0")
    implementation ("com.intuit.ssp:ssp-android:1.1.0")

    //lottie animation
    implementation ("com.airbnb.android:lottie:6.0.0")

    //glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    //shimmer
//    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    //tap target view
    implementation ("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    //worker
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    //In app review
//    implementation("com.google.android.play:review:2.0.1")
//    implementation("com.google.android.play:review-ktx:2.0.1")
    implementation("com.google.code.gson:gson:2.10.1")

    //Camera X
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("com.vanniktech:android-image-cropper:4.5.0")
}