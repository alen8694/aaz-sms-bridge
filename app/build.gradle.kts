plugins { id("com.android.application") }

android {
    namespace = "com.aaz.smsbridge"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aaz.smsbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "1.11.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.10.1")
    testImplementation("junit:junit:4.13.2")
}
