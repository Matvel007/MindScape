import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mindscape.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mindscape.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            val signingPropertiesFile = rootProject.file("key/local.properties")
            val signingKeystoreFile = rootProject.file("key/release-key.jks")
            if (signingPropertiesFile.exists() && signingKeystoreFile.exists()) {
                val signingProperties = Properties().apply {
                    signingPropertiesFile.inputStream().use(::load)
                }
                val storePassword = signingProperties.getProperty("RELEASE_STORE_PASSWORD", "")
                val keyAlias = signingProperties.getProperty("RELEASE_KEY_ALIAS", "")
                val keyPassword = signingProperties.getProperty("RELEASE_KEY_PASSWORD", "")
                if (storePassword.isNotBlank() && keyAlias.isNotBlank() && keyPassword.isNotBlank()) {
                    signingConfig = signingConfigs.create("localRelease") {
                        storeFile = signingKeystoreFile
                        this.storePassword = storePassword
                        this.keyAlias = keyAlias
                        this.keyPassword = keyPassword
                    }
                }
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("MindScape-1.0.2-release.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.security.crypto)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
