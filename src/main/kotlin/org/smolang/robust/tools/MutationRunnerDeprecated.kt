package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.domainSpecific.reasoner.OwlFileHandler
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import org.smolang.robust.mutant.DefinedMutants.*
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

// class to run the main mutation algorithm
@Deprecated("Use mutation runner with configuration file instead.")
open class MutationRunnerDeprecated(
    private val seedFile : File?,
    private val outputFile : File?,
    private val maskFile: File?,
    private val mutationFile: File?,
    private val numberMutations: Int,
    private val overwriteOutput: Boolean,
    private val isOwlDocument: Boolean,
    private val selectionSeed: Int,
    private val printMutationSummary: Boolean
) {

    // set of default mutations that are used if no other mutations are specified
    private val defaultMutation = listOf(
        AddRelationMutation::class,
        ChangeRelationMutation::class,
        AddInstanceMutation::class,
        RemoveStatementMutation::class,
        RemoveNodeMutation::class,
    )

    // create selection of mutations that can be applied
    open fun getDefaultMutations() = defaultMutation

    // main method to perform mutation
    // returns, if mutation was successful or not
    fun mutate() : MutationOutcome {
        // get seed
        if (seedFile == null) {
            mainLogger.error("You need to provide a seed knowledge graph")
            return MutationOutcome.INCORRECT_INPUT
        }

        val seed = getSeed(seedFile, isOwlDocument) ?: return MutationOutcome.INCORRECT_INPUT

        // get mask
        val mask: RobustnessMask? =
            if (maskFile == null)
                EmptyRobustnessMask()
            else
                getMask(maskFile)

        if (mask == null)
            return MutationOutcome.INCORRECT_INPUT


        // check output
        val outputPath: File = if(outputFile == null) {
            mainLogger.error("Please provide an output file to save the mutated knowledge graph to.")
            return MutationOutcome.INCORRECT_INPUT
        } else if(outputFile.exists() && !overwriteOutput){
            mainLogger.error("Output file ${outputFile.path} does already exist. " +
                    "Please choose a different name or set \"--overwrite\" flag.")
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

        // get mutations (from file or use default)
        val mutations = if (mutationFile == null) {
            mainLogger.info("No mutations are provided as input. Using the default mutation operators.")
            getDefaultMutations().map { mutationClass -> AbstractMutation(mutationClass) }
        } else {
            getMutationOperations(mutationFile) ?: return MutationOutcome.INCORRECT_INPUT
        }

        // use random selection of a mutation. Select mutation operator based on seed for random selector
        val generator = Random(selectionSeed)

        // try to mutate until a mutation that conforms to mask is found
        var outcome = MutationOutcome.NOT_VALID
        while (outcome == MutationOutcome.NOT_VALID) {
            val ms = MutationSequence()
            for (j in 1..(numberMutations)) {
                val mutation = mutations.random(generator)
                ms.addAbstractMutation(mutation)
            }
            outcome = singleMutation(seed, outputPath, ms, mask)
        }
        return outcome
    }

    // apply one mutation to the input KG
    // argument: candidateMutations to apply
    private fun singleMutation(
        seedKG: Model,
        outputPath: File,
        ms: MutationSequence,
        mask: RobustnessMask
    ) : MutationOutcome {

        // create mutator and apply mutation
        val m = Mutator(ms)
        val res = m.mutate(seedKG)

        // check, if result conforms to mask; if not --> abort
        if (!mask.validate(res))
            return MutationOutcome.NOT_VALID

        // safe result
        mainLogger.info("Saving mutated knowledge graph to $outputFile")
        if (isOwlDocument)
            OwlFileHandler().saveOwlDocument(res, outputFile!!)
        else
            RDFDataMgr.write(outputPath.outputStream(), res, Lang.TTL)

        // print summary, if required
        if (printMutationSummary)
            println("mutation summary:\n" + m.getStringSummary())

        return MutationOutcome.SUCCESS
    }

    // get seed knowledge graph from file
    private fun getSeed(inputFile: File, isOwlDocument : Boolean) : Model? {

        if(!inputFile.exists()){
            mainLogger.error("File ${inputFile.path} for seed knowledge graph does not exist")
            return null
        }

        val seedKG =
            if (isOwlDocument)
                OwlFileHandler().loadOwlDocument(seedFile!!)
            else
                RDFDataMgr.loadDataset(seedFile!!.absolutePath).defaultModel

        return seedKG
    }

    // get robustness mask from file
    private fun getMask(inputFile: File) : RobustnessMask?{
        val mas: RobustnessMask? =
            if(!inputFile.exists()){
                mainLogger.error("File ${inputFile.path} for mask does not exist")
                null
            } else {
                val shapesGraph = RDFDataMgr.loadGraph(inputFile.absolutePath)
                RobustnessMask(Shapes.parse(shapesGraph))
            }
        return mas
    }

    // get mutation operators from file
    private fun getMutationOperations(inputFile: File) : List<AbstractMutation>?{
        val mutations: List<AbstractMutation>? =
            if(!inputFile.exists()){
                mainLogger.error("File ${inputFile.path} for mutations does not exist")
                null
            } else  {
                val input = RDFDataMgr.loadDataset(inputFile.absolutePath).defaultModel
                val parser = RuleParser(input)
                parser.getAllRuleMutations()
            }
        return mutations
    }

}

