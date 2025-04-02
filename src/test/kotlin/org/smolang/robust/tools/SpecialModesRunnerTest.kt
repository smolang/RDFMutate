package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File

class SpecialModesRunnerTest : StringSpec() {
    init {
        "test EL mutation" {
            val outcome1 = SpecialModesRunner().elMutation(
                seedFile =  File("src/test/resources/reasoners/ore_ont_155.owl"),
                outputFile = File("src/test/resources/swrl/temp.ttl"),
                numberMutations = 5,
                overwriteOutput = true,
                isOwlDocument = true,
                selectionSeed = 42,
                printMutationSummary = false
            )

            outcome1 shouldBe MutationOutcome.SUCCESS

            // invalid arguments (seed file does not exist
            val outcome2 = SpecialModesRunner().elMutation(
                seedFile =  File("src/test/resources/reasoners/this_file_does_not_exist.owl"),
                outputFile = File("src/test/resources/swrl/temp.ttl"),
                numberMutations = 5,
                overwriteOutput = true,
                isOwlDocument = true,
                selectionSeed = 42,
                printMutationSummary = false
            )

            outcome2 shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "test pipe inspection" {
            SpecialModesRunner().testMiniPipes(
                seedPath = "src/test/resources/PipeInspection/miniPipes.ttl"
            )
        }
    }
}