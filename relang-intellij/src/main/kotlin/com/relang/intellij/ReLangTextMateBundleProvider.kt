package com.relang.intellij

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ReLangTextMateBundleProvider : TextMateBundleProvider {

    companion object {
        private val LOG = Logger.getInstance(ReLangTextMateBundleProvider::class.java)
    }

    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        return try {
            val bundlePath = extractBundle()
            if (bundlePath != null) {
                LOG.info("ReLang TextMate bundle loaded from: $bundlePath")
                listOf(TextMateBundleProvider.PluginBundle("relang", bundlePath))
            } else {
                LOG.warn("Failed to extract ReLang TextMate bundle")
                emptyList()
            }
        } catch (e: Exception) {
            LOG.error("Error loading ReLang TextMate bundle", e)
            emptyList()
        }
    }

    private fun extractBundle(): Path? {
        val systemPath = PathManager.getSystemPath()
        val bundleDir = Path.of(systemPath, "relang-textmate-bundle")
        val syntaxesDir = bundleDir.resolve("syntaxes")

        try {
            Files.createDirectories(syntaxesDir)

            // Extract package.json to bundle root
            extractResource("/package.json", bundleDir.resolve("package.json"))

            // Extract grammar to syntaxes subfolder
            extractResource("/syntaxes/relang.tmLanguage.json", syntaxesDir.resolve("relang.tmLanguage.json"))

            LOG.info("ReLang TextMate bundle extracted to: $bundleDir")
            return bundleDir
        } catch (e: Exception) {
            LOG.error("Failed to extract TextMate bundle", e)
            return null
        }
    }

    private fun extractResource(resourcePath: String, targetPath: Path) {
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        stream.use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
