package org.smolang.robust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.file
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.domainSpecific.geo.GeoScenarioGenerator
import org.smolang.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.smolang.robust.domainSpecific.suave.*
import org.smolang.robust.mutant.*
import org.smolang.robust.sut.MiniPipeInspection
import java.io.File
import kotlin.random.Random
import kotlin.system.exitProcess

val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val source by argument().file()
    private val shaclMaskFile by option("--shacl","-s", help="Gives a mask, defined by a set of SHACL shapes").file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val mainMode by option().switch(
        "--scen_geo" to "geo", "-sg" to "geo",
        "--scen_suave" to "suave", "-sv" to "suave",
        "--scen_test" to "test", "-st" to "test",
        "--issre_graph" to "issre", "-ig" to "issre",
        "--free" to "free",       "-f" to "free",
    ).default("free")


    override fun run() {

        when (mainMode){
            "geo" -> {
                val shapes = parseShapes(shaclMaskFile)
                //generateGeoScenarios()
                //runGeoGenerator(shapes)
                evaluateGeoMask(shapes)
            }
            "suave" -> {
                val shapes = parseShapes(shaclMaskFile)
                //runSuaveGenerator(shapes)
                evaluateSuaveMask(shapes)
            }
            "issre" -> generateIssreGraph()
            "test" -> {
                // test installation
                testMiniPipes()
            }
            else -> testMutations()
        }


    }


    private fun parseShapes(shapeFile : File?) : Shapes?{
        val shapes: Shapes? =
            if(shapeFile != null && !shapeFile!!.exists()){
                println("File ${shapeFile!!.path} does not exist")
                exitProcess(-1)
            } else if(shapeFile != null) {
                val shapesGraph = RDFDataMgr.loadGraph(shapeFile!!.absolutePath)
                Shapes.parse(shapesGraph)
            } else null
        return shapes
    }


    //TODO: add a way to add
    private fun testMutations() {

        val shapes: Shapes? = if(shaclMaskFile != null && !shaclMaskFile!!.exists()){
            println("File ${shaclMaskFile!!.path} does not exist")
            exitProcess(-1)
        } else if(shaclMaskFile != null) {
            val shapesGraph = RDFDataMgr.loadGraph(shaclMaskFile!!.absolutePath)
            Shapes.parse(shapesGraph)
        } else null

        if(!source.exists()){
            println("File ${source.path} does not exist")
            exitProcess(-1)
        }

        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel

        val mask = RobustnessMask(verbose, shapes)

        // test configuration stuff

            //val m = Mutator(listOf(AddInstanceMutation::class, RemoveAxiomMutation::class), verbose)
            val ms = MutationSequence(verbose)
            //ms.addRandom(listOf(RemoveSubclassMutation::class))

            val mf = ModelFactory.createDefaultModel()
            val st = mf.createStatement(
                mf.createResource("http://smolang.org#B"),
                mf.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
                mf.createResource("http://smolang.org#A")
            )
            val config = SingleStatementConfiguration(st)

            val r = mf.createResource("http://smolang.org#XYZ")
            val config2 = SingleResourceConfiguration(r)

            val st3 = mf.createStatement(
                mf.createResource("http://smolang.org#B"),
                mf.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf"),
                mf.createResource("http://smolang.org#XYZ")
            )
            val config3 = SingleStatementConfiguration(st3)

            val config4 = StringAndResourceConfiguration("http://smolang.org#newIndividual", r)

            val segment = mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val config5 = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)

            ms.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, config5)

            ms.addWithConfig(RemoveSubclassMutation::class, config)

            ms.addWithConfig(AddAxiomMutation::class, config3)

            ms.addRandom(listOf(AddInstanceMutation::class))

            ms.addWithConfig(AddInstanceMutation::class, config4)

            val m = Mutator(ms, verbose)

            //this is copying before mutating, so we must not copy one more time here
            val res = m.mutate(input)

            //XXX: the following ignores blank nodes
            val valid = m.validate(res, mask)
            println("result of validation: $valid")
            if(verbose) res.write(System.out, "TTL")
    }


    private fun testMiniPipes() {
        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        val pi1 = MiniPipeInspection()

        // run without mutations
        pi1.readOntology(input)
        pi1.doInspection()
        println("everything inspected?: " + pi1.allInfrastructureInspected())

        // mutated ontology with "add pipe" at segment1
        println("\nApply mutation to ontology")
        val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
        val configSegment = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)
        val msSegment = MutationSequence(verbose)
        msSegment.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, configSegment)
        val mSegment = Mutator(msSegment, verbose)
        val resSegment = mSegment.mutate(input)

        val pi2 = MiniPipeInspection()
        pi2.readOntology(resSegment)
        pi2.doInspection()
        println("everything inspected?: " + pi2.allInfrastructureInspected())

        if(!pi1.allInfrastructureInspected() || pi2.allInfrastructureInspected())
            throw Exception("Mutation Generator does not work as expected")
    }


    fun runSuaveGenerator(shapes: Shapes?) {
        val sg = SuaveTestCaseGenerator(true)
        val numberOfMutants = 30
        val numberOfMutations = 2
        val ratioDomainDependent = 0.0
        val mask = RobustnessMask(verbose, shapes)
        val nameOfMutants = "temp"
        val saveMutants = true
        sg.generateSuaveMutants(
            numberOfMutants,
            numberOfMutations,
            ratioDomainDependent,
            mask,
            nameOfMutants,
            saveMutants)
    }

    fun runGeoGenerator(shapes: Shapes?) {
        val gg = GeoTestCaseGenerator(false)
        val numberOfMutants = 100
        val numberOfMutations = 2
        val mask = RobustnessMask(verbose, shapes)
        val nameOfMutants = "temp"
        val saveMutants = true
        gg.generateGeoMutants(
            numberOfMutants,
            numberOfMutations,
            mask,
            nameOfMutants,
            saveMutants)
    }

    // evaluates provided shapes agains the suave test runs
    fun evaluateSuaveMask(shapes: Shapes?) {
        // new mask
        val mask = RobustnessMask(verbose, shapes)

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

    // evaluates provided shapes agains the geo test runs
    fun evaluateGeoMask(shapes: Shapes?) {
        val mask = RobustnessMask(verbose, shapes)

        val relevantSutRuns = listOf(
            "sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
        )
        val detailedEvaluationOutput = true

        mask.checkAgainstOntologies(
            relevantSutRuns,
            detailedEvaluationOutput)
    }

    // creates scenarios, i.e. different layerings, for the geo simulator
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
        SuaveEvaluationGraphGenerator(false).generateGraph(numberOfMutants, outputFile)
    }
}


fun main(args: Array<String>) = Main().main(args)