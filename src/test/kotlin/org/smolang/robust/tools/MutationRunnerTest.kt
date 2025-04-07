package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.tools.reasoning.ReasoningBackend
import java.io.File


class MutationRunnerTest : StringSpec() {
    init {
        "simple parsing from rule file" {
            val configFile = File("src/test/resources/configs/simpleSWRLconfig.yaml")
            val runner = MutationRunner(configFile)

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check if changes happened according to mutation
            val result = RDFDataMgr.loadDataset("src/test/resources/swrl/temp.ttl").defaultModel

            val pRelations = result.listStatements(
                null,
                result.getProperty("http://www.ifi.uio.no/tobiajoh/swrlTest#p"),
                null as RDFNode?
            ).toSet()

            pRelations.size shouldBe 2

            pRelations.contains(result.createStatement(
                result.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b"),
                result.getProperty("http://www.ifi.uio.no/tobiajoh/swrlTest#p"),
                result.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#c")
            )) shouldBe true
        }
    }

    init {
        "no config file provided" {
            val runner = MutationRunner(null)
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "can not override output" {
            val runner = MutationRunner(File("src/test/resources/configs/outputAlreadyExists.yaml"))
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "no strategy name" {
            val runner = MutationRunner(File("src/test/resources/configs/noStrategyName.yaml"))
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            val runner2 = MutationRunner(File("src/test/resources/configs/noStrategyNameStrict.yaml"))
            val outcome2 = runner2.mutate()

            // mutation was successful
            outcome2 shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }


    init {
        "conflicting mutation specification" {
            val runner = MutationRunner(File("src/test/resources/configs/conflictingMutations.yaml"))
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "seed file does not exist" {
            val runner = MutationRunner(File("src/test/resources/configs/noSeedFile.yaml"))
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "mutation file does not exist" {
            val runner = MutationRunner(File("src/test/resources/configs/noMutationFile.yaml"))
            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }


    init {
        "test default mutation with mask" {
            val outputPath = "src/test/resources/abc/temp2.ttl"
            val runner = MutationRunner(File("src/test/resources/configs/simpleMaskConfig.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check that result really adheres to mask
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

            val shapes = Shapes.parse(RDFDataMgr.loadGraph("abc/mask.ttl"))
            val mask = RobustnessMask(shapes, ReasoningBackend.HERMIT)

            mask.validate(result) shouldBe true
        }
    }

    init {
        "test name of class as mutation" {
            val outputPath = "src/test/resources/abc/temp3.ttl"
            val runner = MutationRunner(File("src/test/resources/configs/classMutation.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check that result really adheres to mask
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

            val shapes = Shapes.parse(RDFDataMgr.loadGraph("abc/mask.ttl"))
            val mask = RobustnessMask(shapes, ReasoningBackend.HERMIT)

            mask.validate(result) shouldBe true
        }
    }

    init {
        "name of class without prefix as mutation" {
            val outputPath = "src/test/resources/abc/temp3.ttl"
            val runner = MutationRunner(File("src/test/resources/configs/classMutation.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check that result really adheres to mask
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

            val shapes = Shapes.parse(RDFDataMgr.loadGraph("abc/mask.ttl"))
            val mask = RobustnessMask(shapes, ReasoningBackend.HERMIT)

            mask.validate(result) shouldBe true
        }
    }

    init {
        "name of class of mutation does not exist" {
            val runner = MutationRunner(File("src/test/resources/configs/classMutationError.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "condition is missing in config file" {
            val runner = MutationRunner(File("src/test/resources/configs/maskConditionMissing.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "multiple sources of mutation operators are combined" {
            val outputPath = "src/test/resources/geo/temp2.ttl"
            val runner = MutationRunner(File("src/test/resources/configs/multipleMaskFiles.yaml"))

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check that result really adheres to mask
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

            val shapes = Shapes.parse(RDFDataMgr.loadGraph("geo/generatedMask.ttl"))
            val mask = RobustnessMask(shapes, ReasoningBackend.NONE)

            mask.validate(result) shouldBe true

        }
    }

    init {
        "mask file does not exist" {
            val runner = MutationRunner(File("src/test/resources/configs/nonExistentMaskFile.yaml"))
            val runnerStrict = MutationRunner(File("src/test/resources/configs/nonExistentMaskFileStrict.yaml"))

            runner.mutate() shouldBe MutationOutcome.SUCCESS
            runnerStrict.mutate() shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "mask file broken" {
            val runner = MutationRunner(File("src/test/resources/configs/brokenMaskFile.yaml"))

            runner.mutate() shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }


    init {
        "input ontology is owl" {
            val configFile = File("src/test/resources/configs/elMutation.yaml")
            val runner = MutationRunner(configFile)

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS
        }
    }

}