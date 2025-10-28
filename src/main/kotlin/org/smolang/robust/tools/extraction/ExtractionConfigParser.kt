package org.smolang.robust.tools.extraction

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.KgFormatType
import org.smolang.robust.tools.MutationConfig
import org.smolang.robust.tools.OutputKG
import java.io.File

class ExtractionConfigParser(private val configFile: File?) {
    fun getConfig() : ExtractionConfig?{
        if (configFile == null) {
            mainLogger.error("Configuration file not provided.")
            return null
        }

        if (!configFile.exists()) {
            mainLogger.error("Configuration file does not exist.")
            return null
        }

        val extractionConfig = try {
            Yaml.default.decodeFromStream<ExtractionConfig>(configFile.inputStream())
        }
        catch (e : Exception)  {
            mainLogger.error("Configuration file could not be parsed. Raised exception: $e")
            return null
        }

        return extractionConfig
    }
}

@Serializable
data class ExtractionConfig(
    val jar_location: String,
    val kg_files: List<String>,
    val parameters: ExtractionParameters,
    val output: OutputKG
)

@Serializable
data class ExtractionParameters(
    val min_rule_match: Int,
    val min_head_match: Int,
    val min_confidence: Double,
    val max_rule_length: Int
)
