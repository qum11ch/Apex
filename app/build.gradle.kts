plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.f1app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.f1app"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "1.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation ("com.firebaseui:firebase-ui-storage:9.1.1")
    implementation("com.google.firebase:firebase-storage")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("com.google.android.material:material:1.14.0")
    implementation ("io.github.ShawnLin013:number-picker:2.4.13")
    implementation ("com.google.firebase:firebase-auth")
    implementation ("androidx.preference:preference:1.2.1")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.github.blongho:worldCountryData:${rootProject.extra["worldcountrydataVersion"]}")
    implementation ("com.qcloud.cos:avif:1.1.6")
    implementation ("com.squareup.retrofit2:converter-jackson:3.0.0")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    implementation ("com.fasterxml.jackson.core:jackson-core:2.22.3")
    implementation ("com.fasterxml.jackson.core:jackson-annotations:2.22")
    implementation ("com.google.code.gson:gson:2.14.0")
    implementation ("org.jsoup:jsoup:1.23.2")
    implementation ("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation("com.google.firebase:firebase-database:23.0.0")
    implementation (platform("com.google.firebase:firebase-bom:35.0.1"))
    implementation ("com.google.firebase:firebase-functions")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
    annotationProcessor ("androidx.annotation:annotation:1.11.0")
    implementation ("com.github.bumptech.glide:glide:5.0.9")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.9")
    implementation("androidx.appcompat:appcompat:1.8.0")
    annotationProcessor ("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}