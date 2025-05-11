plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.f1app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.f1app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
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
    }
}

dependencies {
    implementation ("com.firebaseui:firebase-ui-storage:9.0.0")
    implementation("com.google.firebase:firebase-storage")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-beta01")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("io.github.ShawnLin013:number-picker:2.4.13")
    implementation ("com.google.firebase:firebase-auth")
    implementation ("androidx.preference:preference:1.2.1")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.github.blongho:worldCountryData:${rootProject.extra["worldcountrydataVersion"]}")
    implementation ("com.qcloud.cos:avif:1.0.0")
    implementation ("com.squareup.retrofit2:converter-jackson:2.7.2")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.10.3")
    implementation ("com.fasterxml.jackson.core:jackson-core:2.10.3")
    implementation ("com.fasterxml.jackson.core:jackson-annotations:2.10.3")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("org.jsoup:jsoup:1.10.3")
    implementation ("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.gms:play-services-maps:19.1.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    annotationProcessor ("androidx.annotation:annotation:1.9.1")
    implementation ("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.11.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
}