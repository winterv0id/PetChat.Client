plugins {
    alias(libs.plugins.android.application)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.petchat.messenger"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.petchat.messenger"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "SERVER_API_ADDR", "\"http://192.168.1.77:8080\"")
            buildConfigField("String", "CLIENT_NAME", "\"petchat client v1.0.0d\"")
            buildConfigField("String", "CLIENT_KEY", "\"8b3e41dcb87bdb9457f43740\"")
        }
        release {
            optimization {
                enable = false
            }
            buildConfigField("String", "SERVER_API_ADDR", "\"https://api.petchat.com/\"")
            buildConfigField("String", "CLIENT_NAME", "\"petchat client v1.0.0\"")
            buildConfigField("String", "CLIENT_KEY", "\"8b3e41dcb87bdb9457f43740\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildToolsVersion = "37.0.0"

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)


    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.core)
    implementation(libs.firebase.messaging)
    //implementation(libs.firebase.crashlytics)
    //implementation(libs.firebase.analytics)

    implementation(libs.room.runtime)
    implementation(libs.androidx.room.rxjava2)
    implementation(libs.hilt.android)
    annotationProcessor(libs.room.compiler)
    annotationProcessor(libs.hilt.android.compiler)
    implementation(libs.slf4j.android)

    implementation(libs.glide)
    implementation(libs.glide.transformations)

    implementation(libs.httpclient5)
    implementation(libs.gson)
    implementation(libs.signalr)
    implementation(libs.jackson.databind)


    implementation(fileTree("./libs") { include("*.jar", "*.aar") })

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    testImplementation(libs.robolectric.robolectric.v4141)
    testImplementation(libs.core.v161)
}