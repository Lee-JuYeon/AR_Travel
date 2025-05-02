plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jupond.artravel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jupond.artravel"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // OpenCV 라이브러리 추가 (로컬 모듈로 포함된 경우)
    implementation(project(":opencv"))

    // Rajawali 3D 엔진 추가
    implementation("org.rajawali3d:rajawali:1.1.970")

    // Tiny OBJ Loader
    implementation("com.github.javagl:Obj:0.3.0")

    // Apache Commons Math
    implementation("org.apache.commons:commons-math3:3.6.1")

    // JTransforms (필요시)
    implementation("com.github.wendykierp:JTransforms:3.1")
}