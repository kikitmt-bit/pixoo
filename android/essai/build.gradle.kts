// « Pixoo essai » : petite appli du téléphone pour vérifier la brique sur le vrai cadre.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.kiki.pixoo.essai"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.kiki.pixoo.essai"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.${versionCode}"
    }

    signingConfigs {
        create("pixoo") {  // toujours la même signature : les mises à jour s'installent par-dessus
            storeFile = file("../pixoo.keystore")
            storePassword = "pixooessai"
            keyAlias = "pixoo"
            keyPassword = "pixooessai"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("pixoo")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":pixoo"))
}
