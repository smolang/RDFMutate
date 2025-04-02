package org.smolang.robust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.smolang.robust.mutant.*
import org.smolang.robust.tools.MutationOutcome
import org.smolang.robust.tools.MutationRunner
import org.smolang.robust.tools.SpecialModesRunner
import kotlin.random.Random

// logger for this application
val mainLogger: Logger = LoggerFactory.getLogger("org.smolang.robust.OntoMutate")
// random number generator
val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val seedFile by option(
        "--seedKG" ,"-g", help="Knowledge graph to mutate, defined by an RDF file"
    ).file()
    private val shaclMaskFile by option(
        "--shacl","-s", help="Gives a mask, defined by a set of SHACL shapes"
    ).file()
    private val swrlMutationFile by option(
        "--mutations", "--swrl", help = "Mutation operators that are considered."
    ).file()
    private val numberMutations by option(
        "--num_mut", "-nm", help="Number of mutation operators to apply. Default = 1"
    ).int().default(1)
    private val selectionSeed by option(
        "--selection_seed", help="Seed for random selector of which mutation to apply. Default = 2"
    ).int().default(2)
    private val printMutationSummary by option(
        "--print-summary", help="Prints a string summary of the applied mutation. Default = false"
    ).flag(default = false)
    private val isOwlDocument by option(
        "--owl", help="Set to true, if input is OWL ontology (e.g. in functional syntax). Default = false"
    ).flag(default = false)
    private val outputFile by option("--out", "-o", help="Give name for mutated Knowledge graph.").file()
    private val overwriteOutput by option(
        "--overwrite",
        help="Indicates if the output knowledge graph should be replaced, if it already exists. Default = false"
    ).flag(default = false)
    private val mainMode by option(help="Options to run specialized modes of this program. Default = \"--mutate\"").switch(
        "--mutate" to "mutate", "-m" to "mutate",
        "--scen_geo" to "geo", "-sg" to "geo",
        "--el-mutate" to "elReasoner", "-se" to "elReasoner",
        "--scen_suave" to "suave", "-sv" to "suave",
        "--scen_test" to "test", "-st" to "test",
        "--issre_graph" to "issre", "-ig" to "issre",
        "--el-graph" to "elGraph",
        "--parse-swrl" to "swrl"
    ).default("mutate")



    override fun run() {

        when (mainMode){
            "mutate" -> {
                defaultMutation()
            }
            "geo" -> {
                SpecialModesRunner().runGeoGenerator()
            }
            "suave" -> {
                SpecialModesRunner().runSuaveGenerator()
            }
            "elReasoner" -> {
                elMutation()
            }
            "issre" -> SpecialModesRunner().generateIssreGraph()
            "elGraph" -> SpecialModesRunner().generateElReasonerGraph()
            "test" -> {
                // test installation
                SpecialModesRunner().testMiniPipes()
            }
            "swrl" -> SpecialModesRunner().loadSwrlMutations()
            else -> SpecialModesRunner().testMiniPipes()
        }


    }


    // standard function to perform the mutation
    private fun defaultMutation() {
        val runner = MutationRunner(
            seedFile,
            outputFile,
            shaclMaskFile,
            swrlMutationFile,
            numberMutations,
            overwriteOutput,
            isOwlDocument,
            selectionSeed,
            printMutationSummary
        )

        val outcome = runner.mutate()

        if (outcome == MutationOutcome.FAIL)
            mainLogger.error("Creating mutation failed. No mutant was created.")

        if (outcome == MutationOutcome.INCORRECT_INPUT)
            mainLogger.error("Mutation could not be performed because input arguments are incorrect.")

        if (outcome == MutationOutcome.SUCCESS)
            mainLogger.info("Mutation was created successfully.")
    }

    // calls special version of mutation tailored towards EL mutations
    private fun elMutation() {
        SpecialModesRunner().elMutation(
            seedFile,
            outputFile,
            numberMutations,
            overwriteOutput,
            isOwlDocument,
            selectionSeed,
            printMutationSummary
        )
    }
}


fun main(args: Array<String>) = Main().main(args)