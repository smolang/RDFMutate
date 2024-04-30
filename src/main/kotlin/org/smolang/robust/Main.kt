package org.smolang.robust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.smolang.robust.domainSpecific.geo.GeoScenarioGenerator
import org.smolang.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.smolang.robust.domainSpecific.suave.*
import org.smolang.robust.mutant.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mutant.RemoveSubclassMutation
import org.smolang.robust.sut.MiniPipeInspection
import kotlin.random.Random
import kotlin.system.exitProcess

val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val source by argument().file()
    private val contractFile by argument().file()
    private val shaclContractFile by option("--shacl","-s", help="Gives a second contract, defined by a set of SHACL shapes").file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val rounds by option("--rounds","-r", help="Number of mutations applied to input. Default = 1.").int().default(1)
    private val mainMode by option().switch(
        "--scen_geo" to "geo", "-sg" to "geo",
        "--scen_suave" to "suave", "-sv" to "suave",
        "--scen_test" to "test", "-st" to "test",
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
            "test" -> testMiniPipes()
            else -> testMutations()
        }


    }



    fun testMutations() {

        val shapes: Shapes? = if(shaclContractFile != null && !shaclContractFile!!.exists()){
            println("File ${shaclContractFile!!.path} does not exist")
            exitProcess(-1)
        } else if(shaclContractFile != null) {
            val shapesGraph = RDFDataMgr.loadGraph(shaclContractFile!!.absolutePath)
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

        val contract = MutantMask(verbose, shapes, contained)

        // test configuration stuff

        //
        var n = 0
        val onlyOneGeneration = true    // we only execute one run
        while(true) {
            println("\n generation ${n++}")
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

            val config4 = StringAndResourceConfiguration("newIndividual", r)

            val segment = mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val config5 = org.smolang.robust.domainSpecific.auv.AddPipeSegmentConfiguration(segment)

            ms.addWithConfig(org.smolang.robust.domainSpecific.auv.AddPipeSegmentMutation::class, config5)

            //ms.addWithConfig(RemoveAxiomMutation::class, config)
            ms.addWithConfig(RemoveSubclassMutation::class, config)

            ms.addWithConfig(AddAxiomMutation::class, config3)

            //ms.addWithConfig(AddInstanceMutation::class, config2)
            ms.addRandom(listOf(AddInstanceMutation::class))

            ms.addWithConfig(AddInstanceMutation::class, config4)

            val m = Mutator(ms, verbose)

            //this is copying before mutating, so we must not copy one more time here
            val res = m.mutate(input)

            //XXX: the following ignores blank nodes
            val valid = m.validate(res, contract)
            println("result of validation: $valid")
            if(valid) {
                if(verbose) res.write(System.out, "TTL")
                break
            }

            if (onlyOneGeneration) {
                if (verbose) res.write(System.out, "TTL")
                break
            }
        }



    }


    fun testMiniPipes() {
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



    fun runSuaveGenerator(contractPath: String) {
        val sg = SuaveTestCaseGenerator(true)
        sg.generateSuaveMutants(10, contractPath)
    }

    fun runGeoGenerator(contractFile: String, shapes: Shapes?) {
        val gg = GeoTestCaseGenerator(false)
        gg.generateGeoMutants(contractFile, shapes)
    }

    fun evaluateSuaveContract(contractPath : String, shapes: Shapes?) {
        // new contract
        val contract = MutantMask(verbose, shapes, RDFDataMgr.loadDataset(contractPath).defaultModel)

        contract.checkAgainstOntologies(
            listOf(
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave02_2024_03_27_11_52.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave03_2024_03_27_15_11.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave04_2024_03_29_14_15.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlyGeneric03_2024_04_01_11_04.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave05_2024_04_03_17_17.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave06_2024_04_08_09_58.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave07_2024_04_11_08_40.csv",
                "org/smolang/robust/sut/suave/oracle_mutatedOnt_onlySuave08_2024_04_15_08_39.csv"
                ),
            true)
    }

    fun evaluateGeoContract(contractPath : String,shapes: Shapes?) {
        // new contract
        val contract = MutantMask(verbose, shapes,RDFDataMgr.loadDataset(contractPath).defaultModel, useReasonerContainment=true)

        contract.checkAgainstOntologies(
            listOf(
                "org/smolang/robust/sut/geo/benchmark_runs/mutations/oracle_mutatedOnt_secondTest_2024_03_26_09_24.csv"
            ),
            true)
    }

    fun generateGeoScenarios(shapes: Shapes?) {
        val geoGenerator = GeoScenarioGenerator()
        geoGenerator.generateScenarios(10)
    }


}


fun main(args: Array<String>) = Main().main(args)