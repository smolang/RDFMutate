package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import org.smolang.robust.tools.reasoning.ReasoningBackend
import org.smolang.robust.tools.ruleMutations.RuleParser
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class MutationRunner(configFile : File?) {

    val config = ConfigParser(configFile).getConfig()

    // if true, errors in parts of config, e.g. parsing of some operators / mask parts leads to overall abort
    val strictParsing = config?.strict_parsing ?: true

    // main function to extract information from config file and generate mutants
    fun mutate() : MutationOutcome {
        if (config == null) {
            mainLogger.error("Configuration does not exist. Mutation is not possible.")
            return MutationOutcome.INCORRECT_INPUT
        }

        // get seed knowledge graph
        val seedKG = getSeed(File(config.seed_graph.file), config.seed_graph.type) ?: return MutationOutcome.INCORRECT_INPUT

        // get mask
        val mask : RobustnessMask = getMask() ?: return MutationOutcome.INCORRECT_INPUT

        // extract output files; check output path
        val outputFiles = mutableListOf<File>()
        val outputParent = File(config.output_graph.file).parentFile
        if (config.number_of_mutants == 1) {
            // only one mutant file provided
            val outputFile = File(config.output_graph.file)
            if (outputFile.exists() && !config.output_graph.overwrite) {
                mainLogger.error(
                    "Output file ${outputFile.path} does already exist. " +
                            "Please choose a different name or set \"overwrite\" value to \"true\"."
                )
                return MutationOutcome.INCORRECT_INPUT
            }
            outputFiles.add(outputFile) // add file name, if everything is valid
        }
        else {
            val filePattern = config.output_graph.file // pattern provided in config file
            val positionEnding = filePattern.indexOfLast {it == '.'}    // position of the dot indication file type
            val noType = (positionEnding == -1) // no type information found
            // remove everything except ending
            val filePrefix = if (noType) filePattern else filePattern.removeRange(positionEnding, filePattern.length)
            val fileType = if (noType) "" else filePattern.removeRange(0, positionEnding)

            // generate as many file names as required
            for (i in 0..config.number_of_mutants-1) {
                val fileName = "${filePrefix}$i$fileType"
                val outputFile = File(fileName)
                // check if output file exists
                if (outputFile.exists() && !config.output_graph.overwrite) {
                    mainLogger.error(
                        "Output file ${outputFile.path} (generated from pattern) does already exist. " +
                                "Please choose a different name or set \"overwrite\" value to \"true\"."
                    )
                    return MutationOutcome.INCORRECT_INPUT
                }
                // file name is fine --> add to list
                outputFiles.add(outputFile)
            }
        }

        // check if output directory exists and create it, if necessary
        if (outputParent == null) {
            mainLogger.error("No directory for the output file could be determined. This indicates an error in the " +
                    "path of the specified output file.")
            return MutationOutcome.INCORRECT_INPUT
        }
        Files.createDirectories(outputParent.toPath())

        // collect mutations
        val mutations = mutableListOf<AbstractMutation>()
        if (config.mutation_operators.isEmpty())
            mainLogger.warn("No mutation operators provided.")

        for (mutation in config.mutation_operators) {
            // only exactly one of the two options is allowed to be set
            if (!((mutation.module != null) xor  (mutation.resource != null))) {
                mainLogger.error("Mutation operators do need exactly one argument. Either a class representing the " +
                        "operator XOR a file that contains mutation operators.")
                return MutationOutcome.INCORRECT_INPUT
            }

            if (mutation.module != null) {
                // parse all operators of this module
                for (operator in mutation.module.operators) {
                    val extractedMutationOperator = getMutationOperator(mutation.module.location, operator.className)
                    if (extractedMutationOperator != null)
                        mutations.add(extractedMutationOperator)
                    else if (strictParsing) {
                        // mutations could not be parsed
                        mainLogger.error("Could not parse mutations with name ${operator.className} in module " +
                                "${mutation.module.location}.")
                        return MutationOutcome.INCORRECT_INPUT
                    }
                }
            }
            else if (mutation.resource != null){
                val mutationsFromFile = getMutationOperators(File(mutation.resource.file), mutation.resource.syntax)

                if (mutationsFromFile != null) {
                    mutations.addAll(mutationsFromFile)
                }
                else if (strictParsing) {
                    // mutations could not be parsed
                    mainLogger.error("Could not parse mutations from file ${mutation.resource.file}")
                    return MutationOutcome.INCORRECT_INPUT
                }

            }
        }

        // get mutation strategy
        val strategy = getStrategy(mutations, config.number_of_mutations) ?: return MutationOutcome.INCORRECT_INPUT

        // iterate over all mutants that need to be generated
        var generatedKGs = 0
        for (outputFile in outputFiles) {
            // iterate while either mask satisfied or strategy done
            // call: generate mutant
            var foundValid = false
            var mutant: Model? = null
            var m: Mutator? = null
            while (!foundValid) {
                // check, if strategy can provide a new sequence to try
                if (!strategy.hasNextMutationSequence()) {
                    mainLogger.warn("Strategy can not generate enough mutants. Stopped after generating $generatedKGs " +
                            "mutants. (${outputFiles.size} mutants requested)")
                    return MutationOutcome.FAIL
                }

                val mutationSequence = strategy.getNextMutationSequence()
                m = Mutator(mutationSequence)
                mutant = m.mutate(seedKG)
                foundValid = mask.validate(mutant)
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

    // get mutation operator by class name
    private fun getMutationOperator(module: String, className: String) : AbstractMutation? {
        val mutationClass : KClass<out Mutation> = try {
            val kotlinClass = Class.forName("$module.$className").kotlin
            if (kotlinClass.isSubclassOf(Mutation::class))
                kotlinClass as KClass<out Mutation>
            else
                return null
        }
        catch (e : ClassNotFoundException){
            mainLogger.warn("Class for mutation operator $className in module $module can not be found. " +
                    "I am going to ignore this mutation operator. Exception: $e")
            return null
        }

        return AbstractMutation(mutationClass)

    }

    // get mutation operators from file
    private fun getMutationOperators(fileName: File, type : MutationOperatorFormat) : List<AbstractMutation>? {
        val parser = MutationFileParserFactory(type).getParser(fileName)
        return parser.getAllAbstractMutations()
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
                KgFormatType.OWL -> OwlOntologyInterface().loadOwlDocument(inputFile)
            }

        return seedKG
    }

    // get mutation strategy w.r.t. to mutation operators and desired number of mutations
    private fun getStrategy(mutationOperators : List<AbstractMutation>,
                            numberMutations : Int) : MutationStrategy? {
        if (config?.strategy == null && strictParsing){
            mainLogger.error("Strategy is missing.")
            return null
        }

        val selectionSeed : Int = config?.strategy?.seed ?: MutationStrategy.DEFAULT_SEED

        // check type of strategy
        // default: random strategy
        return when (config?.strategy?.name) {
            MutationStrategyName.RANDOM -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
            null -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
        }
    }

    // extracts mask
    private fun getMask() : RobustnessMask? {
        // get reasoning backend (if provided in configuration)
        val reasonerBackend = if (config?.condition == null) {
            // no condition specified

            if (strictParsing) { // strict parsing --> enforce an argument
                mainLogger.error("Condition or reasoning value is missing in configuration.")
                return null
            }

            ReasoningBackend.NONE
        } else if (!config.condition.reasoning.consistency) {
            ReasoningBackend.NONE
        } else if (config.condition.reasoning.reasoner == null){
                mainLogger.info("No reasoner backend found. Use default reasoner (Openllet)")
                ReasoningBackend.OPENLLET
        }
        else {
            config.condition.reasoning.reasoner
        }

        val maskFiles = config?.condition?.masks
        // no shape provided --> mask only considers reasoning
        if (maskFiles.isNullOrEmpty())
            return EmptyRobustnessMask(reasonerBackend)

        // collect graphs from all files and combine them
        val emptyModel = ModelFactory.createDefaultModel()
        val maskModel = maskFiles.fold(emptyModel) { maskModel : Model?, maskFileName ->
            val maskFile = File(maskFileName.file)
            if (!maskFile.exists()) {
                mainLogger.error("File ${maskFile.path} for mask does not exist")
                if (strictParsing) null else maskModel   // don't add anything if file does not exist
            } else {
                try {
                    val shapesModel = RDFDataMgr.loadModel(maskFile.absolutePath)
                    maskModel?.add(shapesModel)
                } catch (e: Exception) {
                    mainLogger.error("Could not parse mask shapes in file ${maskFile.path}")
                    if (strictParsing) null else maskModel
                }
            }
        }
        // if some error occurred during parsing of the mask files
        if (maskModel == null)
            return  null

        // parse shapes and create mask
        val maskShapes = Shapes.parse(maskModel)
        return RobustnessMask(maskShapes, reasonerBackend)
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
