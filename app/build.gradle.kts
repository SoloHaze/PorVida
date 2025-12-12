import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

android {
    namespace = "com.porvida"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.porvida"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Placeholder for Weather API key (user will set value later)
        manifestPlaceholders += mapOf("WEATHER_API_KEY" to "")
    }

    // Signing configuration for release (values come from gradle.properties)
    val releaseStoreFile: String? = project.findProperty("RELEASE_STORE_FILE") as String?
    val releaseStorePassword: String? = project.findProperty("RELEASE_STORE_PASSWORD") as String?
    val releaseKeyAlias: String? = project.findProperty("RELEASE_KEY_ALIAS") as String?
    val releaseKeyPassword: String? = project.findProperty("RELEASE_KEY_PASSWORD") as String?

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrEmpty()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias ?: ""
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    // Inject BuildConfig fields from local.properties for secrets like OPENAI_API_KEY
    val localProps = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localFile.inputStream().use { this.load(it) }
    }
    val openAiKey: String = (localProps.getProperty("OPENAI_API_KEY") ?: "")
    val geminiKey: String = (localProps.getProperty("GEMINI_API_KEY") ?: "")
    val weatherKey: String = (
        (project.findProperty("WEATHER_API_KEY") as String?)
            ?: localProps.getProperty("WEATHER_API_KEY")
            ?: System.getenv("WEATHER_API_KEY")
            ?: ""
    )

    // Update manifest placeholder with resolved weather key
    defaultConfig {
        manifestPlaceholders["WEATHER_API_KEY"] = weatherKey
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
            buildConfigField("String", "WEATHER_API_KEY", "\"$weatherKey\"")
        }
        getByName("release") {
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
            buildConfigField("String", "WEATHER_API_KEY", "\"$weatherKey\"")
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    //Para side bar
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui-viewbinding:1.7.2")
    implementation("androidx.compose.material:material-icons-extended")
    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:2.9.4")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:2.9.4")
    implementation("androidx.navigation:navigation-ui:2.9.4")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.9.4")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // LifecycleScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Google Maps / Play Services Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Bcrypt hashing
    implementation("at.favre.lib:bcrypt:0.10.2")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Espresso Intents for verifying navigation during login
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    // Core test utilities (ApplicationProvider, etc.)
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Google Pay / Play Services Wallet
    implementation("com.google.android.gms:play-services-wallet:18.0.0")

    // Networking: OkHttp + logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlinx Serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit Barcode Scanning (for QR codes)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

        // Retrofit y Gson Converter
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Corrutinas para trabajo asincrónico
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Image loading for weather condition icons
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ---------------- Test dependencies ----------------
    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    // MockK
    testImplementation("io.mockk:mockk:1.13.10")
    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    // AndroidX core-testing (LiveData / Architecture components helpers)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // Room in-memory database for unit tests
    testImplementation("androidx.room:room-testing:2.6.1")
    // JUnit 5 engine (Kotest runs on JUnit Platform)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    // JUnit Jupiter API for @Test and @DisplayName
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")


tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

}