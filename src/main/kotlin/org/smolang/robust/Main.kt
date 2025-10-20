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
import org.smolang.robust.tools.*
import kotlin.random.Random

// logger for this application
val mainLogger: Logger = LoggerFactory.getLogger("org.smolang.robust.RDFMutate")
// random number generator
val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val configFile by option(
        "--config", help = "Configuration file using yaml format."
    ).file()
    private val mainMode by option(help="Options to run specialized modes of this program. Default = \"--mutate\"").switch(
        "--mutate" to "mutate", "-m" to "mutate",
        "--scen_test" to "test", "-st" to "test",
        "--performance-test" to "performance",
        "--performance-test-limited" to "performance-simple",
        "--operator-extraction" to "operator-extraction"
    ).default("mutate")

    override fun run() {

        when (mainMode){
            "mutate" -> {
                defaultMutation()
            }
            "test" -> {
                // test installation
                SpecialModesRunner().testMiniPipes()
            }
            "performance" -> {
                SpecialModesRunner().performanceEvaluation()
            }
            "performance-simple" -> {
                SpecialModesRunner().performanceEvaluation(restricted = true)
            }
            "operator-extraction" -> {
                SpecialModesRunner().testRuleExtraction()
            }
            else -> SpecialModesRunner().testMiniPipes()
        }
    }


    // standard function to perform the mutation
    private fun defaultMutation() {
        val runner = MutationRunner(configFile)

        val outcome = runner.mutate()

        if (outcome == MutationOutcome.FAIL)
            mainLogger.error("Creating mutants failed. The requested number of mutants could not be created.")

        if (outcome == MutationOutcome.INCORRECT_INPUT)
            mainLogger.error("Mutations could not be performed because elements in configuration file are incorrect.")

        if (outcome == MutationOutcome.SUCCESS)
            mainLogger.info("Mutants were created successfully.")
    }

}


fun main(args: Array<String>) = Main().main(args)