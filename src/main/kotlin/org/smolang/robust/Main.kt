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
import java.io.FileOutputStream
import kotlin.random.Random
import kotlin.system.exitProcess

val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val source by argument().file()
    private val contractFile by argument().file()
    private val shaclMaskFile by option("--shacl","-s", help="Gives a second contract, defined by a set of SHACL shapes").file()
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
                //generateGeoScenarios(shapes)
                //runGeoGenerator("sut/geo/contracts/contract1.ttl",shapes)
                //evaluateGeoContract("sut/geo/contracts/contract1.ttl",shapes)
            }
            "suave" ->{
                //testSuave()
                //runSuaveGenerator("sut/suave/contracts/contract7.owl",shapes)
                //evaluateSuaveContract("sut/suave/contracts/contract7.owl",shapes)
            }
            "issre" -> generateIssreGraph()
            "test" -> testMiniPipes()
            else -> testMutations()
        }


    }


    private fun parseShapes(shapeFile : File) : Shapes?{
        val shapes: Shapes? = if(shapeFile != null && !shapeFile!!.exists()){
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

        if(!contractFile.exists()){
            println("File ${contractFile.path} does not exist")
            exitProcess(-1)
        }
        val contained = RDFDataMgr.loadDataset(contractFile.absolutePath).defaultModel

        val contract = RobustnessMask(verbose, shapes, contained)

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
            val valid = m.validate(res, contract)
            println("result of validation: $valid")
            if(verbose) res.write(System.out, "TTL")
    }


    private fun testMiniPipes() {
        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        val pi = MiniPipeInspection()

        // run without mutations
        pi.readOntology(input)
        pi.doInspection()
        println("everything inspected?: " + pi.allInfrastructureInspected())

        // mutated ontology with "add pipe" at segment1
        println("\nApply mutation to ontology")
        val segment = input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
        val configSegment = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)
        val msSegment = MutationSequence(verbose)
        msSegment.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, configSegment)
        val mSegment = Mutator(msSegment, verbose)
        val resSegment = mSegment.mutate(input)

        pi.readOntology(resSegment)
        pi.doInspection()
        println("everything inspected?: " + pi.allInfrastructureInspected())





        // mutated ontology with deletion of animal and infrastructure are disjoint
        // some implementation work needed: remove axiom mutation + more sophisticated reasoning in algorithm to really
        // use an ontological negation
        val st = input.createStatement(
            input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#Animal"),
            input.createProperty("http://www.w3.org/2002/07/owl#disjointWith"),
            input.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#Infrastructure")
        )
        val configAnimal = SingleStatementConfiguration(st)




    }

    fun testSuave() {
        val input = RDFDataMgr.loadDataset("src/main/suave/suave_ontologies/suave_original_with_imports.owl").defaultModel

        val ms = MutationSequence(verbose)


        for (i in 0..10) {
            ms.addRandom(ChangeSolvesFunctionMutation::class)
            ms.addRandom(AddQAEstimationMutation::class)
            ms.addRandom(RemoveQAEstimationMutation::class)
            ms.addRandom(ChangeQualityAttributTypeMutation::class)
            ms.addRandom(ChangeHasValueMutation::class)
            ms.addRandom(ChangeQAComparisonOperatorMutation::class)
            ms.addRandom(AddNewThrusterMutation::class)
        }

        val m = Mutator(ms, verbose)
        val output = m.mutate(input)

        //val output = m.mutate(input)
        //RDFDataMgr.write(File("examples/test2.ttl").outputStream(), output, Lang.TTL)
    }



    fun runSuaveGenerator(maskPath: String) {
        val sg = SuaveTestCaseGenerator(true)
        val mask = RobustnessMask(verbose, null, RDFDataMgr.loadDataset(maskPath).defaultModel)
        val numberOfMutants = 30
        val numberOfMutations = 2
        val ratioDomainDependent = 0.0
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

    fun runGeoGenerator(contractFile: String, shapes: Shapes?) {
        val gg = GeoTestCaseGenerator(false)
        val numberOfMutants = 100
        val numberOfMutations = 2
        val nameOfMutants = "temp"
        gg.generateGeoMutants(
            numberOfMutants,
            numberOfMutations,
            contractFile,
            shapes,
            nameOfMutants)
    }

    fun evaluateSuaveContract(contractPath : String, shapes: Shapes?) {
        // new contract
        val mask = RobustnessMask(verbose, shapes, RDFDataMgr.loadDataset(contractPath).defaultModel)

        mask.checkAgainstOntologies(
            listOf(
                "sut/suave/oracle_mutatedOnt_onlySuave02_2024_03_27_11_52.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave03_2024_03_27_15_11.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave04_2024_03_29_14_15.csv",
                "sut/suave/oracle_mutatedOnt_onlyGeneric03_2024_04_01_11_04.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave05_2024_04_03_17_17.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave06_2024_04_08_09_58.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave07_2024_04_11_08_40.csv",
                "sut/suave/oracle_mutatedOnt_onlySuave08_2024_04_15_08_39.csv"
                ),
            true)
    }

    fun evaluateGeoContract(contractPath : String,shapes: Shapes?) {
        // new contract
        //val mask = RobustnessMask(verbose, shapes,RDFDataMgr.loadDataset(contractPath).defaultModel, useReasonerContainment=true)

        val mask = RobustnessMask(verbose, shapes,RDFDataMgr.loadDataset(contractPath).defaultModel)

        mask.checkAgainstOntologies(
            listOf(
                "sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
            ),
            true)
    }

    fun generateGeoScenarios(shapes: Shapes?) {
        val geoGenerator = GeoScenarioGenerator()
        geoGenerator.generateScenarios(10)
    }

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
    }

    fun generateIssreGraph() {
        val numberOfMutants = 100
        val numberOfMutations = 2
        val nameOfMutants = "temp"
        val saveMutants = false
        val ratioDomainDependent1 = 1.0
        val ratioDomainDependent2 = 0.0

        val outputFile = File("sut/suave/evaluation/attemptsPerMask.csv")

        // lists to collect number of attempts for domain-independent and -specific operators
        val listAttemptsDI : MutableList<Int> = mutableListOf()
        val listAttemptsDS : MutableList<Int> = mutableListOf()
        val ids = listOf(0,1,2,3,4,5,6,7)
        for (id in ids) {
            println("create mutants for mask with id $id")
            val maskFile = File("sut/suave/masks/mask$id.ttl")
            val shapesGraph = RDFDataMgr.loadGraph(maskFile!!.absolutePath)

            val mask = RobustnessMask(verbose, Shapes.parse(shapesGraph), ModelFactory.createDefaultModel())


            // generate domain-independent mutants
            val sg = SuaveTestCaseGenerator(false)
            val attemptsDI = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainDependent1,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDI.add(attemptsDI)

            // generate domain-specific mutants
            val attemptsDS = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations,
                ratioDomainDependent2,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDS.add(attemptsDS)

        }

        // output results to csv file
        FileOutputStream(outputFile).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("contract,number mutants,attemptsDI,ratioDI,attemptsDS,ratioDS")
            writer.newLine()
            for (id in ids) {
                val attemptsDI = listAttemptsDI[id]
                val ratioDI = attemptsDI.toFloat() / numberOfMutants
                val attemptsDS = listAttemptsDS[id]
                val ratioDS = attemptsDS.toFloat() / numberOfMutants
                writer.write("$id,$numberOfMutants,$attemptsDS,$ratioDS,$attemptsDI,$ratioDI")
                writer.newLine()
            }
            writer.close()
            println("writetoFile $outputFile")
        }

    }


}


fun main(args: Array<String>) = Main().main(args)