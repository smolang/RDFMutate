package org.smolang.robust.tools

import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.domainSpecific.geo.GeoScenarioGenerator
import org.smolang.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.smolang.robust.domainSpecific.reasoner.OwlEvaluationGraphGenerator
import org.smolang.robust.domainSpecific.suave.SuaveEvaluationGraphGenerator
import org.smolang.robust.domainSpecific.suave.SuaveTestCaseGenerator
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import org.smolang.robust.mutant.DefinedMutants.*
import org.smolang.robust.sut.auv.MiniPipeInspection
import java.io.File

// class to run special modes of the tool
class SpecialModesRunner {

    fun testMiniPipes(seedPath: String = "examples/miniPipes.ttl" ) {
        //if(!seed.exists()) throw Exception("Input file $seed does not exist")
        //val input = RDFDataMgr.loadDataset(seed.absolutePath).defaultModel

        val input = RDFDataMgr.loadDataset(seedPath).defaultModel
        val pi1 = MiniPipeInspection()

        // run without mutations
        pi1.readOntology(input)
        pi1.doInspection()
        println("everything inspected?: " + pi1.allInfrastructureInspected())

        // mutated ontology with "add pipe" at segment1
        println("\nApply mutation to ontology")
        val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
        val configSegment = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)
        val msSegment = MutationSequence()
        msSegment.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, configSegment)
        val mSegment = Mutator(msSegment)
        val resSegment = mSegment.mutate(input)

        val pi2 = MiniPipeInspection()
        pi2.readOntology(resSegment)
        pi2.doInspection()
        println("everything inspected?: " + pi2.allInfrastructureInspected())

        if(!pi1.allInfrastructureInspected() || pi2.allInfrastructureInspected())
            throw Exception("Mutation Generator does not work as expected")
        else
            println("Mutation Generator works as expected.")
    }

    // mutates a seed KG with the mutation operators suitable for EL ontologies
    fun elMutation(
        seedFile : File?,
        outputFile : File?,
        numberMutations: Int,
        overwriteOutput: Boolean,
        isOwlDocument: Boolean,
        selectionSeed: Int,
        printMutationSummary: Boolean
    ) : MutationOutcome {
        // create selection of mutations that can be applied
        val runner = ELMutationRunner(
            seedFile,
            outputFile,
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

        return  outcome
    }


}