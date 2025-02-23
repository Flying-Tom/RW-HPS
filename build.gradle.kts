/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

buildscript {
    setRepositories()

    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${Versions.dokkaVersion}")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version Versions.kotlinVersion
    id("org.jetbrains.dokka") version Versions.dokkaVersion
}

setRepositories()

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    setRepositories()

    tasks.jar {
        manifest {
            attributes(mapOf("Implementation-Vendor" to "RW-HPS Team"))
            attributes(mapOf("Build-Jar-Time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date().time)))
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }

    tasks.withType<JavaCompile> {
        /* 使用Java11做标准语法并编译 */
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()

        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf(
            "-Xlint:unchecked", "-Werror",
            "-Xdiags:verbose", "-Werror",
            "-Xlint:deprecation", "-Werror"
        ))
    }
}

subprojects {
    afterEvaluate {
        if (project.path == ":ASM-Framework") configureDokka()
        if (project.path == ":TimeTaskQuartz") configureDokka()
        if (project.path == ":Server-Core") configureDokka()
    }
}
rootProject.configureDokka()

fun Project.configureDokka() {
    val isRoot = (this@configureDokka == rootProject)
    if (!isRoot) {
        this@configureDokka.apply(plugin = "org.jetbrains.dokka")
    }

    dependencies {
        dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Versions.dokkaVersion}")
        dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:${Versions.dokkaVersion}")
    }

    tasks.withType<AbstractDokkaTask>().configureEach {
        pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
            this.footerMessage = """Copyright 2020-${
                LocalDateTime.now().year
            } <a href="https://github.com/RW-HPS">RW-HPS Technologies</a> and contributors.
            Source code:
            <a href="https://github.com/RW-HPS/RW-HPS">GitHub</a>
            """.trimIndent()
        }
    }

    tasks.withType<DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            perPackageOption {
                //skipDeprecated.set(true)
            }
        }
    }

    if (isRoot) {
        tasks.named<AbstractDokkaTask>("dokkaHtmlMultiModule").configure {
            outputDirectory.set(
                rootProject.projectDir.resolve("Java-Doc/pages/snapshot")
            )
        }
    }
}