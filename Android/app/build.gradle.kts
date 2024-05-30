// buildConfig 사용
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cavss.artravel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cavss.artravel"

        // AR Optional apps must declare minSdkVersion >= 14.
        // AR Required apps must declare minSdkVersion >= 24.
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // buildConfig 사용
        buildConfigField("String", "firebase_web_client_id", getKey("firebase_web_client_id"))
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true // view binding 추가.
        dataBinding = true // data binding 추가.
        buildConfig = true // buildConfig 사용
    }

}

dependencies {

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //ARCore 라이브러리
    implementation("com.google.ar:core:1.43.0")

    //obj 파일을 읽기 위한 라이브러리
    implementation("de.javagl:obj:0.2.1")

    // bottom navigation
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-runtime-ktx:2.7.7")

    // fireabse auth
    implementation("com.google.android.gms:play-services-auth:21.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid")

    //firebase realtime database
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

    // Map, osmdriod
    implementation("org.osmdroid:osmdroid-android:6.1.11")

    // glide
    implementation("com.github.bumptech.glide:glide:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")

}

// buildConfig 사용
fun getKey(propertyKey: String): String {
    return gradleLocalProperties(rootDir).getProperty(propertyKey)
}