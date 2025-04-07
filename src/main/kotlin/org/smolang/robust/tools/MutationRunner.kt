package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.domainSpecific.reasoner.OwlFileHandler
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class MutationRunner(private val configFile : File?) {

    val config = ConfigParser(configFile).getConfig()

    fun mutate() : MutationOutcome {
        if (config == null) {
            mainLogger.error("Configuration does not exist. Mutation is not possible.")
            return MutationOutcome.INCORRECT_INPUT
        }

        // get seed knowledge graph
        val seedFile = config.seedGraph.file
        if (seedFile == null) {
            mainLogger.error("You need to provide a seed knowledge graph")
            return MutationOutcome.INCORRECT_INPUT
        }
        val seedKG = getSeed(File(seedFile), config.seedGraph.type) ?: return MutationOutcome.INCORRECT_INPUT

        // get mask
        val mask : RobustnessMask = EmptyRobustnessMask()

        if (mask == null)
            return MutationOutcome.INCORRECT_INPUT

        // check output path
        val outputFile = File(config.outputKG.file)
        val outputPath: File = if(outputFile == null) {
            mainLogger.error("Please provide an output file to save the mutated knowledge graph to.")
            return MutationOutcome.INCORRECT_INPUT
        } else if(outputFile.exists() && !config.outputKG.overwrite){
            mainLogger.error("Output file ${outputFile.path} does already exist. " +
                    "Please choose a different name or set \"overwrite\" value to \"true\".")
            return MutationOutcome.INCORRECT_INPUT
        }  else outputFile

        // check if output directory exists and create it, if necessary
        val outputParent = outputPath.parentFile
        if (outputParent == null) {
            mainLogger.error("No directory for the output file could be determined. This indicates an error in the " +
                    "path of the specified output file.")
            return MutationOutcome.INCORRECT_INPUT
        }
        Files.createDirectories(outputParent.toPath())

        // collect mutations
        val mutations = mutableListOf<AbstractMutation>()
        for (mutation in config.mutation_operators) {
            // only exactly one of the two options is allowed to be set
            if (!((mutation.operator != null) xor  (mutation.resource != null))) {
                mainLogger.error("Mutation operators do need exactly one argument. Either a class representing the " +
                        "operator XOR a file that contains mutation operators.")
                return MutationOutcome.INCORRECT_INPUT
            }

            if (mutation.operator != null) {
                val extractedMutationOperator = getMutationOperator(mutation.operator)
                if (extractedMutationOperator != null)
                    mutations.add(extractedMutationOperator)
            }
            else if (mutation.resource != null){
                val mutationsFromFile = getMutationOperators(File(mutation.resource.file))
                if (mutationsFromFile != null)
                    mutations.addAll(mutationsFromFile)
            }
        }

        // get mutation strategy
        val strategy = getStrategy(mutations, config.number_of_mutations)

        // iterate while either mask satisfied or strategy done
        // call: generate mutant
        var foundValid = false
        var mutant : Model? = null
        while (!foundValid) {
            // check, if strategy can provide a new sequence to try
            if (!strategy.hasNextMutationSequence())
                return MutationOutcome.FAIL

            val mutationSequence = strategy.getNextMutationSequence()
            val m = Mutator(mutationSequence)
            mutant = m.mutate(seedKG)
            foundValid = mask.validate(mutant)
        }

        // safe outcome KG
        assert(mutant != null)
        mainLogger.info("Saving mutated knowledge graph to $outputFile")
        exportResult(mutant!!, outputFile, config.outputKG.type)

        return  MutationOutcome.SUCCESS
    }

    private fun getMutationOperator(className: String) : AbstractMutation? {
        val mutationClass : KClass<out Mutation>? = try {
            val kotlinClass = Class.forName(className).kotlin
            if (kotlinClass.isSubclassOf(Mutation::class))
                kotlinClass as KClass<out Mutation>
            else
                null
        }
        catch (e : ClassNotFoundException){
            mainLogger.error("Class for mutation operator can not be found. I am going to ignore this mutation " +
                    "operator. Exception: $e")
            return null
        }

        return if (mutationClass!=null)
            AbstractMutation(mutationClass)
        else
            null
    }

    private fun getMutationOperators(fileName: File) : List<AbstractMutation>? {
        val mutations: List<AbstractMutation>? =
            if(!fileName.exists()){
                mainLogger.error("File ${fileName.path} for mutations does not exist")
                null
            } else  {
                val input = RDFDataMgr.loadDataset(fileName.absolutePath).defaultModel
                val parser = RuleParser(input)
                parser.getAllRuleMutations()
            }
        return mutations
    }

    // get seed knowledge graph from file
    private fun getSeed(inputFile: File, fileType : KgFormatType) : Model? {

        if(!inputFile.exists()){
            mainLogger.error("File ${inputFile.path} for seed knowledge graph does not exist")
            return null
        }

        val seedKG =
            when (fileType) {
                KgFormatType.RDF -> RDFDataMgr.loadDataset(inputFile.absolutePath).defaultModel
                KgFormatType.OWL -> OwlFileHandler().loadOwlDocument(inputFile)
            }

        return seedKG
    }

    private fun getStrategy(mutationOperators : List<AbstractMutation>,
                            numberMutations : Int) : MutationStrategy {
        val selectionSeed : Int = config?.strategy?.seed ?: MutationStrategy.defaultSeed

        // check type of strategy
        // default: random strategy
        return when (config?.strategy?.name) {
            MutationStrategyName.RANDOM -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
            null -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
        }
    }

    // returns if saving was
    private fun exportResult(mutant: Model, outputFile: File, fileType: KgFormatType) {
        when (fileType) {
            KgFormatType.RDF -> RDFDataMgr.write(outputFile.outputStream(), mutant, Lang.TTL)
            KgFormatType.OWL -> OwlFileHandler().saveOwlDocument(mutant, outputFile)
        }
    }
}

enum class MutationOutcome {
    INCORRECT_INPUT, SUCCESS, FAIL, NOT_VALID
}
