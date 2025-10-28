package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.tools.KgFormatType
import org.smolang.robust.tools.OwlOntologyInterface
import java.io.File

// orchestrates the whole operator extraction process
class ExtractionRunner(configFile : File?) {

    val config = ExtractionConfigParser(configFile).getConfig()

    fun extract(): ExtractionOutcome {
        // extract information from config
        if (config == null) {
            mainLogger.error("Configuration does not exist. Extraction of operators is not possible.")
            return ExtractionOutcome.INCORRECT_INPUT
        }
        val jarLocation = config.jar_location
        if (!File(jarLocation).exists()) {
            mainLogger.error("JAR can not be found to extract rules.")
            return ExtractionOutcome.INCORRECT_INPUT
        }

        // check if input files exits
        val kg_files = config.kg_files.map{File(it)}.toSet()
        var all_exist = true
        kg_files.forEach { file ->
            if (!file.exists()) {
                mainLogger.error("Input KG file $file does not exits.")
                all_exist = false
            }
        }
        if (!all_exist)
            return ExtractionOutcome.INCORRECT_INPUT

        // check if output is possible
        if (File(config.output.file).exists() && !config.output.overwrite) {
            mainLogger.error("Output file ${config.output.file} exists already. " +
                    "Consider to set the flag \"overwrite\", if the file should be replaced.")
            return ExtractionOutcome.INCORRECT_INPUT
        }

        val parameters = config.parameters
        val extractorBridge = ExtractorBridge(
            parameters.min_rule_match,
            parameters.min_head_match,
            parameters.min_confidence,
            parameters.max_rule_length,
            jarLocation
        )

        val rules = AssociationRuleExtractor().mineRules(
            extractorBridge,
            kg_files
        )

        val operators = rules?.flatMap { it.getAbstractMutations() }

        val swrlModel = ModelFactory.createDefaultModel() // model to collect all the operators
        operators?.forEach { operator ->
            if (operator.config !is RuleMutationConfiguration){
                mainLogger.error("An error while extracting the operators occurred." +
                        "The configuration of the operator does not have the correct type.")
                return ExtractionOutcome.FAIL
            }
            // add swrl representation of operator
            swrlModel.add((operator.config as RuleMutationConfiguration).asSWRLRule())
        }

        // save rules to file
        if (config.output.type == KgFormatType.RDF)
            RDFDataMgr.write(File(config.output.file).outputStream(), swrlModel, Lang.TTL)
        else
            OwlOntologyInterface().saveOwlDocument(swrlModel, File(config.output.file))



        return ExtractionOutcome.SUCCESS
    }
}

enum class ExtractionOutcome {
    INCORRECT_INPUT, SUCCESS, FAIL
}
