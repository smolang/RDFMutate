package org.smolang.robust.tools

import org.smolang.robust.mainLogger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

class ConfigParser(val configFile: File?) {

    fun testYamlInput() {
        if (configFile == null){
            mainLogger.warn("Config file not provided.")
            return
        }

        val config = Yaml.default.decodeFromStream<Config>(configFile.inputStream())

        println(config.seedGraph.file)
        println(config.seedGraph.type)
        println(config.number_of_mutations)
        println(config.strategy.seed)
    }
}

@Serializable
data class Config(
    val seedGraph: SeedKG,
    val outputKG: OutputKG,
    val strategy: Strategy,
    val number_of_mutations: Int,
    val condition: ConformanceCondition,
    val mutation_operators: List<MutationOperatorConfiguration>
)

@Serializable
data class SeedKG(
    val file: String,
    val type: KgFormatType
)

@Serializable
data class OutputKG(
    val file: String,
    val type: KgFormatType,
    val overwrite: Boolean
)


@Serializable
data class Strategy(
    val name: MutationStrategy,
    val seed: Int=42
)

@Serializable
data class ConformanceCondition(
    val reasoning: ConformanceReasoning,
    val masks: List<MaskConfiguration>
)

@Serializable
data class ConformanceReasoning(
    val consistency: Boolean,
    val reasoner: ReasoningBackend?
)

@Serializable
data class MaskConfiguration(
    val file: String
)

@Serializable
data class MutationOperatorConfiguration(
    val operator: String? = null,
    val resource: MutationOperatorResource? = null
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
enum class MutationStrategy {
    @SerialName("random")
    RANDOM
}

@Serializable
enum class MutationOperatorFormats {
    @SerialName("swrl")
    SWRL
}

