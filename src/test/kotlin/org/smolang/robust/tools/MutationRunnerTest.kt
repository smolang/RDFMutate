package org.smolang.robust.tools

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mutant.RobustnessMask
import java.io.File

class MutationRunnerTest : StringSpec() {

    init {
        "test default mutation without mask" {
            val outputPath = "src/test/resources/swrl/temp.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/swrl/swrlTest.ttl"),
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check if changes happened according to mutation
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

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
        "test default mutation with mask" {
            val outputPath = "src/test/resources/abc/temp2.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/abc/abc.ttl"),
                outputFile = File(outputPath),
                maskFile = File("src/test/resources/abc/mask.ttl"),
                mutationFile = File("src/test/resources/abc/RemoveSubclassRelationMutation.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.SUCCESS

            // read output to see, if it was saved correctly
            // i.e., check that result really adheres to mask
            val result = RDFDataMgr.loadDataset(outputPath).defaultModel

            val shapes = Shapes.parse(RDFDataMgr.loadGraph("abc/mask.ttl"))
            val mask = RobustnessMask(shapes)

            mask.validate(result) shouldBe false
        }

    }

    init {
        "insufficient arguments (missing seed)" {
            val outputPath = "src/test/resources/swrl/temp.ttl"
            val runner = MutationRunner(
                seedFile = null,
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was not successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "insufficient arguments (non-existing seed)" {
            val outputPath = "src/test/resources/swrl/temp.ttl"
            val runner = MutationRunner(
                seedFile = File("file_does_not_exist.ttl"),
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was no successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "insufficient arguments (no output file)" {
            val runner = MutationRunner(
                seedFile = File("src/test/resources/swrl/swrlTest.ttl"),
                outputFile = null,
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "insufficient arguments (non-existent output directorz)" {
            val outputPath = "file_with_incorrect_path.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/swrl/swrlTest.ttl"),
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was not successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT
        }
    }

    init {
        "insufficient arguments (output file can not be overridden)" {
            val outputPath = "src/test/resources/swrl/temp.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/swrl/swrlTest.ttl"),
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("src/test/resources/swrl/swrlTest.ttl"),
                numberMutations = 1,
                overwriteOutput = false,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "insufficient arguments (mutations file does not exist)" {
            val outputPath = "src/test/resources/swrl/temp.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/swrl/swrlTest.ttl"),
                outputFile = File(outputPath),
                maskFile = null,
                mutationFile = File("file_does_not_exist.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }
    }

    init {
        "insufficient arguments (non-existing mask file)" {
            val outputPath = "src/test/resources/abc/temp2.ttl"
            val runner = MutationRunner(
                seedFile = File("src/test/resources/abc/abc.ttl"),
                outputFile = File(outputPath),
                maskFile = File("file_does_not_exist.ttl"),
                mutationFile = File("src/test/resources/abc/RemoveSubclassRelationMutation.ttl"),
                numberMutations = 1,
                overwriteOutput = true,
                isOwlDocument = false,
                selectionSeed = 42,
                printMutationSummary = false
            )

            val outcome = runner.mutate()

            // mutation was successful
            outcome shouldBe MutationOutcome.INCORRECT_INPUT

        }

    }
 }