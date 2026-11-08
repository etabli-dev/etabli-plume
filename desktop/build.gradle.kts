// Copyright 2026 R. Heller
// SPDX-License-Identifier: Apache-2.0

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.0"
}

group = "com.raban.etabli.tex"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test"))
}

// One catalogue for every platform. The desktop app packages the exact asset the
// Android app ships; the iOS bundle holds an identical copy that
// scripts/check_dictionary.py keeps in step. Nothing is copied into this tree.
val dictionaryAsset = layout.projectDirectory.file("../android/app/src/main/assets/Dictionary.json")
val designTokens = layout.projectDirectory.file("../design/coder-design-system.json")

/** Renders the shared 1024 px app icon into the formats jpackage expects. */
abstract class DesktopIcons : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val master: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun render() {
        val source = ImageIO.read(master.get().asFile)
        val dir = outputDir.get().asFile.apply { mkdirs() }

        fun png(size: Int): ByteArray {
            var image = source
            // Halve repeatedly before the final step so small sizes stay smooth.
            while (image.width / 2 >= size) image = scale(image, image.width / 2)
            if (image.width != size) image = scale(image, size)
            return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        }

        dir.resolve("plume.png").writeBytes(png(512))

        // ICO with embedded PNG images (Windows Vista and later).
        val icoSizes = listOf(16, 24, 32, 48, 64, 128, 256)
        val icoImages = icoSizes.map(::png)
        val ico = ByteBuffer.allocate(6 + 16 * icoSizes.size + icoImages.sumOf { it.size })
            .order(ByteOrder.LITTLE_ENDIAN)
        ico.putShort(0).putShort(1).putShort(icoSizes.size.toShort())
        var offset = 6 + 16 * icoSizes.size
        icoSizes.zip(icoImages).forEach { (size, data) ->
            val edge = if (size >= 256) 0 else size
            ico.put(edge.toByte()).put(edge.toByte()).put(0).put(0)
            ico.putShort(1).putShort(32).putInt(data.size).putInt(offset)
            offset += data.size
        }
        icoImages.forEach { ico.put(it) }
        dir.resolve("plume.ico").writeBytes(ico.array())

        // ICNS with embedded PNG images (macOS 10.7 and later).
        val icnsTypes = listOf(
            "icp4" to 16, "icp5" to 32, "ic11" to 32, "ic12" to 64, "ic07" to 128,
            "ic13" to 256, "ic08" to 256, "ic14" to 512, "ic09" to 512, "ic10" to 1024,
        )
        val icnsImages = icnsTypes.map { (type, size) -> type to png(size) }
        val icns = ByteBuffer.allocate(8 + icnsImages.sumOf { 8 + it.second.size })
            .order(ByteOrder.BIG_ENDIAN)
        icns.put("icns".toByteArray(Charsets.US_ASCII)).putInt(icns.capacity())
        icnsImages.forEach { (type, data) ->
            icns.put(type.toByteArray(Charsets.US_ASCII)).putInt(8 + data.size).put(data)
        }
        dir.resolve("plume.icns").writeBytes(icns.array())
    }

    private fun scale(image: BufferedImage, size: Int): BufferedImage {
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(image, 0, 0, size, size, null)
        g.dispose()
        return out
    }
}

val desktopIcons = tasks.register<DesktopIcons>("desktopIcons") {
    master.set(layout.projectDirectory.file("../ios/App/Assets.xcassets/AppIcon.appiconset/AppIcon.png"))
    outputDir.set(layout.buildDirectory.dir("generated/icons"))
}
val iconDir = desktopIcons.flatMap { it.outputDir }

tasks.processResources {
    from(dictionaryAsset)
    from(iconDir) {
        include("plume.png")
        rename { "icon-plume.png" }
    }
}

tasks.test {
    useJUnit()
    inputs.files(designTokens, dictionaryAsset)
    systemProperty("plume.designTokens", designTokens.asFile.absolutePath)
    systemProperty("plume.dictionaryAsset", dictionaryAsset.asFile.absolutePath)
}

// Package with the same JDK 17 toolchain that compiles the app (jlink + jpackage),
// independent of the JDK that happens to run Gradle.
val packagingJdk = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(17)
}

compose.desktop {
    application {
        mainClass = "com.raban.etabli.tex.desktop.MainKt"
        javaHome = packagingJdk.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "etabli-plume"
            packageVersion = "0.1.0"
            description = "Offline reference for typesetting commands"
            vendor = "R. Heller"
            copyright = "Copyright 2026 R. Heller"
            licenseFile.set(layout.projectDirectory.file("../LICENSE"))

            linux {
                iconFile.set(iconDir.map { it.file("plume.png") })
                debMaintainer = "58561665+r-heller@users.noreply.github.com"
                menuGroup = "Office"
                appCategory = "text"
                shortcut = true
            }
            windows {
                iconFile.set(iconDir.map { it.file("plume.ico") })
                menuGroup = "Etabli"
                // Fixed for the lifetime of the app so newer MSIs replace older ones.
                upgradeUuid = "5b3c79b2-d5d5-47cb-8981-63abbba3ab8c"
                perUserInstall = true
                shortcut = true
                menu = true
            }
            macOS {
                // macOS bundle versions need a major version of at least 1.
                packageVersion = "1.0.0"
                iconFile.set(iconDir.map { it.file("plume.icns") })
                bundleID = "com.raban.etabli.tex"
                dockName = "Établi Plume"
            }
        }
    }
}
