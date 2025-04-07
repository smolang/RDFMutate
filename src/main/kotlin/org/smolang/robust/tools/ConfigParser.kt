package org.smolang.robust.tools

import org.smolang.robust.mainLogger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.smolang.robust.mutant.MutationStrategy
import org.smolang.robust.mutant.MutationStrategyName
import org.smolang.robust.tools.reasoning.ReasoningBackend
import java.io.File

class ConfigParser(private val configFile: File?) {

    fun getConfig() : Config?{
        if (configFile == null) {
            mainLogger.error("Configuration file not provided.")
            return null
        }

        if (!configFile.exists()) {
            mainLogger.error("Configuration file does not exist.")
            return null
        }

        val config = try {
            Yaml.default.decodeFromStream<Config>(configFile.inputStream())
        }
        catch (e : Exception)  {
            mainLogger.error("Configuration file could not be parsed. Raised exception: $e")
            return null
        }

        return config
    }
}

@Serializable
data class Config(
    val seed_graph: SeedKG,
    val output_graph: OutputKG,
    val strategy: Strategy? = null,
    val number_of_mutations: Int,
    val condition: ConformanceCondition? = null,
    val mutation_operators: List<MutationOperatorConfiguration> = listOf(),
    val strict_parsing: Boolean = true,
    val print_summary: Boolean = false
)

@Serializable
data class SeedKG(
    val file: String,
    val type: KgFormatType = KgFormatType.RDF
)

@Serializable
data class OutputKG(
    val file: String,
    val type: KgFormatType = KgFormatType.RDF,
    val overwrite: Boolean = false
)


@Serializable
data class Strategy(
    val name: MutationStrategyName = MutationStrategyName.RANDOM,
    val seed: Int=MutationStrategy.DEFAULT_SEED
)

@Serializable
data class ConformanceCondition(
    val reasoning: ConformanceReasoning,
    val masks: List<MaskConfiguration> = listOf()
)

@Serializable
data class ConformanceReasoning(
    val consistency: Boolean = false,
    val reasoner: ReasoningBackend? = null
)

@Serializable
data class MaskConfiguration(
    val file: String
)

@Serializable
data class MutationOperatorConfiguration(
    val module: OperatorModule? = null,
    val resource: MutationOperatorResource? = null
)

@Serializable
data class OperatorModule(
    val location: String,
    val operators: List<OperatorName> = listOf()
)

@Serializable
data class OperatorName(
    val className: String
)


@Serializable
data class MutationOperatorResource(
    val file: String,
    val syntax: MutationOperatorFormats
)

@Serializable
enum class KgFormatType {
    @SerialName("rdf")
    RDF,
    @SerialName("owl")
    OWL
}


@Serializable
enum class MutationOperatorFormats {
    @SerialName("swrl")
    SWRL
}

