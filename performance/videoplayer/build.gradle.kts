/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.getNativeProgramExtension
import org.jetbrains.kotlin.mingwPath

plugins {
    id("compile-benchmarking")
}

val dist = file(findProperty("org.jetbrains.kotlin.native.home") ?: "dist")
val toolSuffix = if (System.getProperty("os.name").startsWith("Windows")) ".bat" else ""
val binarySuffix = getNativeProgramExtension()

val linkerOpts = when {
    PlatformInfo.isMac() -> listOf("-linker-options","-L/opt/local/lib", "-linker-options", "-L/usr/local/lib")
    PlatformInfo.isLinux() -> listOf("-linker-options", "-L/usr/lib/x86_64-linux-gnu", "-linker-options", "-L/usr/lib64")
    PlatformInfo.isWindows() -> listOf("-linker-options", "-L$mingwPath/lib")
    else -> error("Unsupported platform")
}

var includeDirsFfmpeg = emptyList<String>()
var filterDirsFfmpeg = emptyList<String>()
when {
    PlatformInfo.isMac() -> filterDirsFfmpeg = listOf(
        "-headerFilterAdditionalSearchPrefix", "/opt/local/include",
        "-headerFilterAdditionalSearchPrefix", "/usr/local/include"
    )
    PlatformInfo.isLinux() -> filterDirsFfmpeg = listOf(
        "-headerFilterAdditionalSearchPrefix", "/usr/include",
        "-headerFilterAdditionalSearchPrefix", "/usr/include/x86_64-linux-gnu",
        "-headerFilterAdditionalSearchPrefix", "/usr/include/ffmpeg"
    )
    PlatformInfo.isWindows() -> includeDirsFfmpeg = listOf("-compiler-option", "-I$mingwPath/include")
}

var includeDirsSdl = when {
    PlatformInfo.isMac() -> listOf(
        "-compiler-option", "-I/opt/local/include/SDL2",
        "-compiler-option", "-I/usr/local/include/SDL2"
    )
    PlatformInfo.isLinux() -> listOf("-compiler-option", "-I/usr/include/SDL2")
    PlatformInfo.isWindows() -> listOf("-compiler-option", "-I$mingwPath/include/SDL2")
    else -> error("Unsupported platform")
}

compileBenchmark {
    applicationName = "Videoplayer"
    repeatNumber = 10
    buildSteps = mapOf(
        "runCinteropFfmpeg" to listOf("$dist/bin/cinterop$toolSuffix",
            "-o", "$dist/../samples/videoplayer/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-ffmpeg.klib",
            "-def", "$dist/../samples/videoplayer/src/nativeInterop/cinterop/ffmpeg.def"
        ) + filterDirsFfmpeg + includeDirsFfmpeg,

        "runCinteropSdl" to listOf("$dist/bin/cinterop$toolSuffix",
            "-o", "$dist/../samples/videoplayer/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-sdl.klib",
            "-def", "$dist/../samples/videoplayer/src/nativeInterop/cinterop/sdl.def"
        ) + includeDirsSdl,

        "runKonanProgram" to listOf("$dist/bin/konanc$toolSuffix",
            "-ea", "-p", "program",
            "-o", "${buildDir.absolutePath}/program$binarySuffix",
            "-l", "$dist/../samples/videoplayer/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-ffmpeg.klib",
            "-l", "$dist/../samples/videoplayer/build/classes/kotlin/videoPlayer/main/videoplayer-cinterop-sdl.klib",
            "-Xmulti-platform", "$dist/../samples/videoplayer/src/videoPlayerMain/kotlin",
            "-entry", "sample.videoplayer.main"
        ) + linkerOpts
    )
}