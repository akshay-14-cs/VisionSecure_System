plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.vs_project"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vs_project"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    
    // Retrofit & Gson for Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    
    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    
    // Location
    implementation(libs.play.services.location)
    
    // MPAndroidChart for Analytics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // ML Kit Barcode Scanning for QR Codes
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

tasks.register("moveVideos") {
    doLast {
        val rawDir = file("src/main/res/raw")
        rawDir.mkdirs()
        
        val f1 = file("src/main/res/drawable/can_you_make_it_blinkable_gif.mp4")
        if (f1.exists()) {
            f1.renameTo(file("src/main/res/raw/welcome_video_light.mp4"))
        }
        
        val f2 = file("src/main/res/drawable-night/give_for_this_black_also.mp4")
        if (f2.exists()) {
            f2.renameTo(file("src/main/res/raw/welcome_video_dark.mp4"))
        }
        
        val f3 = file("src/main/res/drawable/can_you_make_it_blinkable_gif.gif")
        if (f3.exists()) {
            f3.delete() // Not supported in drawable, remove to prevent error
        }
    }
}

tasks.named("preBuild") {
    dependsOn("moveVideos")
}