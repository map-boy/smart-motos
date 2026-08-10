pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
      authentication {
        create<org.gradle.authentication.http.BasicAuthentication>("basic")
      }
      credentials {
        username = "mapbox"
        password = run {
          val props = java.util.Properties()
          val propsFile = java.io.File(System.getProperty("user.home"), ".gradle/gradle.properties")
          if (propsFile.exists()) propsFile.inputStream().use { props.load(it) }
          props.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN") ?: ""
        }
      }
    }
  }
}
rootProject.name = "Smart Motos"
include(":app")
