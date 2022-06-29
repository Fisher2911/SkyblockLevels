plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "io.github.fisher2911"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.wolfyscript.com/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.incendo.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT")
    compileOnly("com.wolfyscript.customcrafting:customcrafting-spigot:3.16.3.3")
    implementation("cloud.commandframework:cloud-paper:1.7.0")
    implementation("cloud.commandframework:cloud-minecraft-extras:1.7.0")
    implementation("cloud.commandframework:cloud-annotations:1.7.0")
    compileOnly("com.github.angeschossen:LandsAPI:6.5.1")
    implementation("com.github.retrooper.packetevents:spigot:2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.incendo.interfaces:interfaces-core:1.0.0-SNAPSHOT")
    implementation("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    shadowJar {
        relocate("cloud.commandframework", "io.github.fisher2911.skyblocklevels.cloud.commandframework")
        relocate("com.github.retrooper.packetevents", "io.github.fisher2911.skyblocklevels.packetevents.spigot")
        relocate("org.spongepowered.configurate.yaml", "io.github.fisher2911.skyblocklevels.configurate.yml")
        relocate("org.incendo.interfaces", "io.github.fisher2911.skyblocklevels.interfaces")
        archiveFileName.set("SkyblockLevels.jar")

        dependencies {
            exclude(dependency("org.yaml:snakeyaml"))
        }
    }


    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

}