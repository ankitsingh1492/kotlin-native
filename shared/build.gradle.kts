/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.VersionGenerator
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.Properties

plugins {
    id("kotlin")
}

val rootProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").reader().use(::load)
}

val kotlinVersion: String by rootProperties
val konanVersion: String by rootProperties
val kotlinCompilerRepo: String by rootProperties
val buildKotlinCompilerRepo: String by rootProperties


group = "org.jetbrains.kotlin"
version = konanVersion

repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    maven(kotlinCompilerRepo)
    maven(buildKotlinCompilerRepo)
}

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: drop generation of KonanVersion!
val generateCompilerVersion by tasks.registering(VersionGenerator::class) {}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("src/main/kotlin")
    kotlin.srcDir("src/library/kotlin")
    kotlin.srcDir(generateCompilerVersion.get().versionSourceDirectory)
}

tasks.withType<KotlinCompile> {
    dependsOn(generateCompilerVersion)
    kotlinOptions.jvmTarget = "1.8"
}

tasks.clean {
    doFirst {
        val versionSourceDirectory = generateCompilerVersion.get().versionSourceDirectory
        if (versionSourceDirectory.exists()) {
            versionSourceDirectory.delete()
        }
    }
}

tasks.jar {
    archiveName = "shared.jar"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
    api("org.jetbrains.kotlin:kotlin-util-klib:$kotlinVersion")
}
