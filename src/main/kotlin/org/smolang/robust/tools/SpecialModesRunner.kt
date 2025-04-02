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
import org.smolang.robust.sut.MiniPipeInspection
import java.io.File

// class to run special modes of the tool
class SpecialModesRunner {
    private val elReasonerMutations = listOf(
        // -------------Tbox-----------------------
        // declarations
        DeclareClassMutation::class,
        DeclareObjectPropMutation::class,
        DeclareDataPropMutation::class,
        // sub-class axioms
        AddSubclassRelationMutation::class,
        RemoveSubclassRelationMutation::class,
        // equivalent-class axioms
        AddEquivalentClassRelationMutation::class,
        RemoveEquivClassRelationMutation::class,
        // disjoint-class axioms
        AddDisjointClassRelationMutation::class,
        RemoveDisjointClassRelationMutation::class,
        // replace class
        ReplaceClassWithTopMutation::class,
        ReplaceClassWithBottomMutation::class,
        ReplaceClassWithSiblingMutation::class,
        // add properties of object properties
        AddReflexiveObjectPropertyRelationMutation::class,
        AddTransitiveObjectPropertyRelationMutation::class,
        // domains and ranges of properties
        AddObjectPropDomainMutation::class,
        AddDataPropDomainMutation::class,
        RemoveDomainRelationMutation::class,
        AddObjectPropRangeMutation::class,
        AddDataPropRangeMutation::class,
        RemoveRangeRelationMutation::class,
        // property hierarchy
        AddSubObjectPropMutation::class,
        AddSubDataPropMutation::class,
        RemoveSubPropMutation::class,
        AddEquivObjectPropMutation::class,
        AddEquivDataPropMutation::class,
        RemoveEquivPropMutation::class,
        AddPropertyChainMutation::class,
        // complex class expressions
        AddObjectIntersectionOfMutation::class,
        AddELObjectOneOfMutation::class,
        AddObjectSomeValuesFromMutation::class,
        AddObjectHasValueMutation::class,
        AddDataHasValueMutation::class,
        AddObjectHasSelfMutation::class,
        AddELDataIntersectionOfMutation::class,
        AddELDataOneOfMutation::class,
        AddELSimpleDataSomeValuesFromMutation::class,
        // misc
        CEUAMutation::class,
        AddDatatypeDefinition::class,
        AddHasKeyMutation::class,

        // -------------Abox-----------------------
        // individuals
        AddIndividualMutation::class,   // adds owl named individual
        RemoveIndividualMutation::class,
        AddClassAssertionMutation::class,
        RemoveClassAssertionMutation::class,
        // relations between individuals
        AddObjectPropertyRelationMutation::class,
        RemoveObjectPropertyRelationMutation::class,
        AddNegativeObjectPropertyRelationMutation::class,
        RemoveNegativePropertyAssertionMutation::class,     // also applies to data properties
        // equivalence of individuals
        AddSameIndividualAssertionMutation::class,
        RemoveSameIndividualAssertionMutation::class,
        AddDifferentIndividualAssertionMutation::class,
        RemoveDifferentIndividualAssertionMutation::class,
        // data properties
        BasicAddDataPropertyRelationMutation::class,
        RemoveDataPropertyRelationMutation::class,
        AddNegativeDataPropertyRelationMutation::class
    )

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

    fun runSuaveGenerator() {
        val maskFiles = listOf(
            "sut/suave/masks/mask0.ttl",
            "sut/suave/masks/mask1.ttl",
            "sut/suave/masks/mask2.ttl",
            "sut/suave/masks/mask3.ttl",
            "sut/suave/masks/mask4.ttl",
            "sut/suave/masks/mask5.ttl",
            "sut/suave/masks/mask6.ttl",
            "sut/suave/masks/mask7.ttl",
        )



        assert(maskFiles.size == 8)

        for(i in 0..7){
            val shapes = parseShapes(File(maskFiles[i]))
            val sg = SuaveTestCaseGenerator()

            // number of mutants differs for each generation
            val numberOfMutants =
                when (i) {
                    0, 1, 3 -> 10
                    2 -> 20
                    4 -> 40
                    5, 6, 7 -> 30
                    else -> 0
                }
            val numberOfMutations = 2

            // select domain specific / independent based on generation
            val ratioDomainSpecific =
                if (i == 3)
                    0.0
                else
                    1.0

            // use this specific mutation only for first four generations
            val useAddQAMutation = (i <= 4)

            val mask = RobustnessMask(shapes)

            // select name based on which mutations are used
            val nameOfMutants =
                when (i) {
                    3 -> "generation${i}_domain_independent"
                    else -> "generation${i}_domain_specific"
                }

            val saveMutants = true

            sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainSpecific,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants)
        }

    }

    fun runGeoGenerator() {

        val maskFiles = listOf(
            "sut/geo/masks/mask0.ttl",
            "sut/geo/masks/mask1.ttl"
        )

        var id = 0
        for (maskFile in maskFiles) {
            val gg = GeoTestCaseGenerator()
            val numberOfMutants = 100
            val numberOfMutations = 2

            val shapes = parseShapes(File(maskFile))
            val mask = RobustnessMask(shapes)

            val nameOfMutants = "geoMutant$id"
            val saveMutants = true
            gg.generateGeoMutants(
                numberOfMutants,
                numberOfMutations,
                mask,
                nameOfMutants,
                saveMutants
            )

            id += 1
        }
    }

    // evaluates provided shapes against the suave test runs
    fun evaluateSuaveMask(shapes: Shapes?) {
        // new mask
        val mask = RobustnessMask(shapes)

        val relevantSutRuns =  listOf(
            "sut/suave/oracle_mutatedOnt_onlySuave02_2024_03_27_11_52.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave03_2024_03_27_15_11.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave04_2024_03_29_14_15.csv",
            "sut/suave/oracle_mutatedOnt_onlyGeneric03_2024_04_01_11_04.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave05_2024_04_03_17_17.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave06_2024_04_08_09_58.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave07_2024_04_11_08_40.csv",
            "sut/suave/oracle_mutatedOnt_onlySuave08_2024_04_15_08_39.csv"
        )
        val detailedEvaluationOutput = true

        mask.checkAgainstOntologies(
            relevantSutRuns,
            detailedEvaluationOutput)
    }

    // evaluates provided shapes against the geo test runs
    fun evaluateGeoMask(shapes: Shapes?) {
        val mask = RobustnessMask(shapes)

        val relevantSutRuns = listOf(
            "sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
        )
        val detailedEvaluationOutput = true

        mask.checkAgainstOntologies(
            relevantSutRuns,
            detailedEvaluationOutput)
    }

    // creates scenarios, i.e. different layers, for the geo simulator
    fun generateGeoScenarios() {
        val geoGenerator = GeoScenarioGenerator()
        geoGenerator.generateScenarios(10)
    }

    // takes ontologies, i.e. files with axioms and turns them into SHACL shapes
    fun turnContractIntoSHACLShape() {
        val ids = listOf(0,1,2,3,4,5,6,7)
        for (i in ids ) {
            val sg = ShapeGenerator()
            sg.turnAxiomsToShapes("sut/suave/contracts/contract$i.owl")
            sg.saveShapes("sut/suave/masks", "mask$i")
        }

        val sg = ShapeGenerator()

        sg.turnAxiomsToShapes("sut/geo/contracts/contract1.ttl")
        sg.saveShapes("sut/geo/masks", "mask1")

        val sgExample = ShapeGenerator()

        sgExample.turnAxiomsToShapes("examples/contract.ttl")
        sgExample.saveShapes("examples", "mask")
    }

    // generates graph for ISSRE paper
    fun generateIssreGraph() {
        val numberOfMutants = 100
        val outputFile = File("sut/suave/evaluation/attemptsPerMask.csv")
        SuaveEvaluationGraphGenerator().generateGraph(numberOfMutants, outputFile)
    }

    // generates graph for journal extension
    fun generateElReasonerGraph() {
        val inputDirectory = File("sut/reasoners/ontologies_ore")
        val outputFile = File("sut/reasoners/evaluation/inputCoverage.csv")
        OwlEvaluationGraphGenerator().analyzeElInputCoverage(inputDirectory, elReasonerMutations, outputFile)
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


    fun loadSwrlMutations() {
        val input = RDFDataMgr.loadDataset("examples/swrl/swrlTestNegatedClass.ttl").defaultModel

        val parser = RuleParser(input)
        val ruleMutations = parser.getAllRuleMutations()

        for (r in ruleMutations)
            println(r)

        val ms = MutationSequence()
        ms.addAllAbstractMutations(ruleMutations)

        val m = Mutator(ms)
        val res = m.mutate(input)

        //for (s in res.listStatements())
        //    println(s)

        //OwlFileHandler().saveOwlDocument(res, File("examples/swrl/temp.owl"))
        RDFDataMgr.write(File("examples/swrl/temp.owl").outputStream(), res, Lang.TTL)
    }

    private fun parseShapes(shapeFile : File?) : Shapes?{
        val shapes: Shapes? =
            if(shapeFile != null && !shapeFile.exists()){
                mainLogger.warn("File ${shapeFile.path} does not exist")
                null
            } else if(shapeFile != null) {
                val shapesGraph = RDFDataMgr.loadGraph(shapeFile.absolutePath)
                Shapes.parse(shapesGraph)
            } else null
        return shapes
    }
}