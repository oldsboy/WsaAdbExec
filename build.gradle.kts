import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

println("========================================================project.build.gradle.kts starting========================================================")

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "com.oldsboy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

configurations{
    all{
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

//Kotlin 源代码可以与 Java 源代码放在相同文件夹或者不同文件夹中。默认约定是使用不同的文件夹：
//如果不使用默认约定，那么应该更新相应的 sourceSets 属性：
sourceSets.main {
    java.srcDirs("src/main/java", "src/main/kotlin")
}

kotlin {
    kotlinDaemonJvmArgs = listOf("-Xmx486m", "-Xms256m", "-XX:+UseParallelGC")
}

kotlin{
    target {
        attributes {  }
        preset
        platformType.name
    }

    sourceSets{
    }

}

println("========================================================project.build.gradle.kts end========================================================")