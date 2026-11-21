pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "pixoo"
include(":pixoo")   // la brique, réutilisable par toutes les applis
include(":essai")   // petite appli d'essai pour le téléphone
