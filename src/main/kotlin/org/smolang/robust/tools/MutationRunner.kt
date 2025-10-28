package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import java.io.File
import java.nio.file.Files

class MutationRunner(configFile : File?) {

    val config = MutationConfigParser(configFile).getConfig()

    // if true, errors in parts of config, e.g. errors in parsing of some operators / mask parts leads to overall abort
    val strictParsing = config?.strict_parsing ?: true

    // main function to extract information from config file and generate mutants
    fun mutate() : MutationOutcome {
        if (config == null) {
            mainLogger.error("Configuration does not exist. Mutation is not possible.")
            return MutationOutcome.INCORRECT_INPUT
        }

        // extract setup from config
        val mutationSetup = ConfigInterpreter.interpretConfig(config, strictParsing) ?: run {
            mainLogger.error("Configuration could not be parsed. Mutation is not possible.")
            return MutationOutcome.INCORRECT_INPUT
        }

        // check if output directory exists and create it, if necessary
        val outputParent = mutationSetup.outputParent
        Files.createDirectories(outputParent.toPath())

        // iterate over all mutants that need to be generated
        var generatedKGs = 0
        for (outputFile in mutationSetup.outputFiles) {
            // iterate until either mask satisfied or strategy done
            // call: generate mutant
            var foundValid = false
            var mutant: Model? = null
            var m: Mutator? = null
            while (!foundValid) {
                // check, if strategy can provide a new sequence to try
                if (!mutationSetup.strategy.hasNextMutationSequence()) {
                    mainLogger.warn("Strategy can not generate enough mutants. Stopped after generating $generatedKGs " +
                            "mutants. (${mutationSetup.outputFiles.size} mutants requested)")
                    return MutationOutcome.FAIL
                }

                val mutationSequence = mutationSetup.strategy.getNextMutationSequence()
                m = Mutator(mutationSequence)
                mutant = m.mutate(mutationSetup.seedKG)
                foundValid = mutationSetup.mask.validate(mutant)
            }

            // safe outcome KG
            assert(mutant != null && m != null)
            mainLogger.info("Saving mutated knowledge graph to $outputFile")
            exportResult(mutant!!, outputFile, config.output_graph.type)

            generatedKGs += 1   // increase counter

            // print summary, if required
            if (config.print_summary)
                println("mutation summary:\n" + m?.getStringSummary())
        }

        return  MutationOutcome.SUCCESS
    }



    // returns if saving was
    private fun exportResult(mutant: Model, outputFile: File, fileType: KgFormatType) {
        when (fileType) {
            KgFormatType.RDF -> RDFDataMgr.write(outputFile.outputStream(), mutant, Lang.TTL)
            KgFormatType.OWL -> OwlOntologyInterface().saveOwlDocument(mutant, outputFile)
        }
    }

}

enum class MutationOutcome {
    INCORRECT_INPUT, SUCCESS, FAIL, NOT_VALID
}
