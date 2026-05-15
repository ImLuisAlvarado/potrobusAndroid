import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "mx.itson.potrobus"

    buildFeatures {
        buildConfig = true
    }

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "mx.itson.potrobus"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 1. Instanciamos un objeto Properties
        val properties = Properties()

        // 2. Apuntamos al archivo local.properties que está en la raíz del proyecto
        val localPropertiesFile = rootProject.file("local.properties")

        // 3. Verificamos si el archivo existe y lo cargamos en memoria
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        // 4. Creamos una variable dinámica (Placeholder) para el AndroidManifest.xml.
        // Buscará 'MAPS_API_KEY' en el local.properties. Si no lo encuentra, asignará un string vacío.
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: ""

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.retrofit)
    implementation(libs.gson.converter)
    implementation(libs.google.maps)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}