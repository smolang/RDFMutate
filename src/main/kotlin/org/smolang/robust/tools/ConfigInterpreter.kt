package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.EmptyRobustnessMask
import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationStrategy
import org.smolang.robust.mutant.MutationStrategyName
import org.smolang.robust.mutant.RandomMutationStrategy
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.tools.reasoning.ReasoningBackend
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

// interprets a configuration to extract the information for the mutation
class ConfigInterpreter {
    companion object {
        // extracts the setup how to run the mutation
        fun interpretConfig(config: Config, strictParsing: Boolean): MutationSetup? {
            val seedKG = getSeed(
                File(config.seed_graph.file),
                config.seed_graph.type
            ) ?: return null
            val mask = getMask(config, strictParsing) ?: return null
            val outputFiles = getOutputFiles(config) ?: return null
            val outputParent = File(config.output_graph.file).parentFile
            val mutationOperators = getMutationOperators(config, strictParsing) ?: return null
            val strategy = getStrategy(
                config,
                strictParsing,
                mutationOperators,
                config.number_of_mutations) ?: return null


            return MutationSetup(
                seedKG,
                mask,
                outputFiles,
                outputParent,
                mutationOperators,
                strategy
            )
        }

        // get seed knowledge graph from file
        private fun getSeed(inputFile: File, fileType: KgFormatType): Model? {

            if (!inputFile.exists()) {
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

        // extracts mask
        private fun getMask(config: Config, strictParsing: Boolean): RobustnessMask? {
            // get reasoning backend (if provided in configuration)
            val reasonerBackend = if (config.condition == null) {
                // no condition specified

                if (strictParsing) { // strict parsing --> enforce an argument
                    mainLogger.error("Condition or reasoning value is missing in configuration.")
                    return null
                }

                ReasoningBackend.NONE
            } else if (!config.condition.reasoning.consistency) {
                ReasoningBackend.NONE
            } else if (config.condition.reasoning.reasoner == null) {
                mainLogger.info("No reasoner backend found. Use default reasoner (Openllet)")
                ReasoningBackend.OPENLLET
            } else {
                config.condition.reasoning.reasoner
            }

            val maskFiles = config.condition?.masks
            // no shape provided --> mask only considers reasoning
            if (maskFiles.isNullOrEmpty())
                return EmptyRobustnessMask(reasonerBackend)

            // collect graphs from all files and combine them
            val emptyModel = ModelFactory.createDefaultModel()
            val maskModel = maskFiles.fold(emptyModel) { maskModel: Model?, maskFileName ->
                val maskFile = File(maskFileName.file)
                if (!maskFile.exists()) {
                    mainLogger.error("File ${maskFile.path} for mask does not exist")
                    if (strictParsing) null else maskModel   // don't add anything if file does not exist
                } else {
                    try {
                        val shapesModel = RDFDataMgr.loadModel(maskFile.absolutePath)
                        maskModel?.add(shapesModel)
                    } catch (_: Exception) {
                        mainLogger.error("Could not parse mask shapes in file ${maskFile.path}")
                        if (strictParsing) null else maskModel
                    }
                }
            }
            // if some error occurred during parsing of the mask files
            if (maskModel == null)
                return null

            // parse shapes and create mask
            val maskShapes = Shapes.parse(maskModel)
            return RobustnessMask(maskShapes, reasonerBackend)
        }

        // extracts list of output files
        private fun getOutputFiles(config: Config): List<File>? {
            if (config.number_of_mutants == 1) {
                // only one mutant file provided
                val outputFile = File(config.output_graph.file)
                if (outputFile.exists() && !config.output_graph.overwrite) {
                    mainLogger.error(
                        "Output file ${outputFile.path} does already exist. " +
                                "Please choose a different name or set \"overwrite\" value to \"true\"."
                    )
                    return null
                }
                return listOf(outputFile) // add file name, if everything is valid
            }
            else {
                val outputFiles = mutableListOf<File>()
                val filePattern = config.output_graph.file // pattern provided in config file
                val positionEnding = filePattern.indexOfLast {it == '.'}    // position of the dot indication file type
                val noType = (positionEnding == -1) // no type information found
                // remove everything except ending
                val filePrefix = if (noType) filePattern else filePattern.removeRange(positionEnding, filePattern.length)
                val fileType = if (noType) "" else filePattern.removeRange(0, positionEnding)

                // generate as many file names as required
                for (i in 0..<config.number_of_mutants) {
                    val fileName = "${filePrefix}$i$fileType"
                    val outputFile = File(fileName)
                    // check if output file exists
                    if (outputFile.exists() && !config.output_graph.overwrite) {
                        mainLogger.error(
                            "Output file ${outputFile.path} (generated from pattern) does already exist. " +
                                    "Please choose a different name or set \"overwrite\" value to \"true\"."
                        )
                        return null
                    }
                    // file name is fine --> add to list
                    outputFiles.add(outputFile)
                }
                return outputFiles
            }
        }

        // extracts mutation operators
        private fun getMutationOperators(config: Config, strictParsing: Boolean): List<AbstractMutation>? {
            val mutations = mutableListOf<AbstractMutation>()
            if (config.mutation_operators.isEmpty())
                mainLogger.warn("No mutation operators provided.")

            for (mutation in config.mutation_operators) {
                // only exactly one of the two options is allowed to be set
                if (!((mutation.module != null) xor  (mutation.resource != null))) {
                    mainLogger.error("Mutation operators do need exactly one argument. Either a class representing the " +
                            "operator XOR a file that contains mutation operators.")
                    return null
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
                            return null
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
                        return null
                    }

                }
            }
            return mutations
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

        // get mutation strategy w.r.t. to mutation operators and desired number of mutations
        private fun getStrategy(config: Config,
                                strictParsing: Boolean,
                                mutationOperators : List<AbstractMutation>,
                                numberMutations : Int) : MutationStrategy? {
            if (config.strategy == null && strictParsing){
                mainLogger.error("Strategy is missing.")
                return null
            }

            val selectionSeed : Int = config.strategy?.seed ?: MutationStrategy.DEFAULT_SEED

            // check type of strategy
            // default: random strategy
            return when (config.strategy?.name) {
                MutationStrategyName.RANDOM -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
                null -> RandomMutationStrategy(mutationOperators, numberMutations, selectionSeed)
            }
        }
    }
}

// all elements necessary to orchestrate mutation
data class MutationSetup (
    val seedKG: Model,
    val mask: RobustnessMask,
    val outputFiles: List<File>,
    val outputParent: File,
    val mutationOperators: List<AbstractMutation>,
    val strategy: MutationStrategy
)