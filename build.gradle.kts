import java.util.Properties

description = "OAS stub"

val publishPropertiesFile = file("publish.properties")
if (publishPropertiesFile.exists()) {
    publishPropertiesFile.inputStream().buffered().use { s ->
        Properties().apply {
            load(s)
        }
    }.forEach { k, v ->
        ext[k as String] = v
    }
}

allprojects {
    group = group
    version = version

    repositories {
        mavenCentral()
    }
}
