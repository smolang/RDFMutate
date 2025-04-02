package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File

class ELMutationRunnerTest: StringSpec() {
    init {
        "test EL mutations" {
            val runner = ELMutationRunner(
                seedFile =  File("src/test/resources/reasoners/ore_ont_155.owl"),
                outputFile = File("src/test/resources/swrl/temp.ttl"),
                numberMutations = 5,
                overwriteOutput = true,
                isOwlDocument = true,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            outcome shouldBe MutationOutcome.SUCCESS
        }
    }
}