package org.smolang.robust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.domainSpecific.geo.GeoScenarioGenerator
import org.smolang.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.smolang.robust.domainSpecific.reasoner.CoverageEvaluationGraphGenerator
import org.smolang.robust.domainSpecific.reasoner.OwlFileHandler
import org.smolang.robust.domainSpecific.reasoner.OwlOntologyAnalyzer
import org.smolang.robust.domainSpecific.suave.SuaveEvaluationGraphGenerator
import org.smolang.robust.domainSpecific.suave.SuaveOntologyAnalyzer
import org.smolang.robust.domainSpecific.suave.SuaveTestCaseGenerator
import org.smolang.robust.mutant.*
import org.smolang.robust.sut.MiniPipeInspection
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.system.exitProcess


val randomGenerator = Random(2)

class Main : CliktCommand() {
    private val seed by option("--seedKG" ,"-g", help="KG to mutate, defined by an RDF file").file()
    private val shaclMaskFile by option("--shacl","-s", help="Gives a mask, defined by a set of SHACL shapes").file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val numberMutations by option("--num_mut", "-nm", help="Number of mutation operators to apply. Default = 1").int()
    private val selectionSeed by option("--selection_seed", help="seed for random selector of which mutation to apply. Default = 2").int()
    private val printMutationSummary by option("--print-summary", help="Prints a string summary of the applied mutation. Default = false").flag()
    private val owlDocument by option("--owl", help="Set to true, if input is OWL ontology (e.g. in functional syntax). Default = false").flag()
    private val outputFile by option("--out", "-o", help="Give name for mutated KG.").file()
    private val overwriteOutput by option("--overwrite", help="Indicates if output ontology should be replaced, if it already exists. Default = false").flag()
    private val sampleSize by option("--coverage-samples", "--sample-size", help="number of samples used for coverage evaluation. Default = 100").int()
    private val mainMode by option(help="Options to run specialized modes of this program.").switch(
        "--mutate" to "mutate", "-m" to "mutate",
        "--scen_geo" to "geo", "-sg" to "geo",
        "--el-mutate" to "elReasoner", "-se" to "elReasoner",
        "--scen_suave" to "suave", "-sv" to "suave",
        "--scen_test" to "test", "-st" to "test",
        "--issre_graph" to "issre", "-ig" to "issre",
        "--el-graph" to "elGraph",
        "--suave-coverage-graph" to "suaveCoverageGraph",
        "--analyze-minimization" to "minimization",
        "--suave-features" to "suaveFeatures"
    ).default("free")

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


    override fun run() {

        when (mainMode){
            "mutate" -> {
                defaultMutation()
            }
            "geo" -> {
                runGeoGenerator()
            }
            "suave" -> {
                runSuaveGenerator()
            }
            "elReasoner" -> {
                elMutation()
            }
            "issre" -> generateSuaveAttemptsGraph()
            "elGraph" -> generateElCoverageGraphs()
            "suaveCoverageGraph" -> generateSuaveCoverageGraphs()
            "suaveFeatures" -> listSuaveFeatures()
            "test" -> {
                // test installation
                testMiniPipes()
            }
            "minimization" -> analyzeMinimization()
            else -> testMiniPipes()
        }


    }


    private fun parseShapes(shapeFile : File?) : Shapes?{
        val shapes: Shapes? =
            if(shapeFile != null && !shapeFile.exists()){
                println("File ${shapeFile.path} does not exist")
                exitProcess(-1)
            } else if(shapeFile != null) {
                val shapesGraph = RDFDataMgr.loadGraph(shapeFile.absolutePath)
                Shapes.parse(shapesGraph)
            } else null
        return shapes
    }

    private fun defaultMutation() {
        // create selection of mutations that can be applied
        val candidateMutations = listOf(
            AddRelationMutation::class,
            ChangeRelationMutation::class,
            AddInstanceMutation::class,
            RemoveStatementMutation::class,
            RemoveNodeMutation::class,
        )

        singleMutation(candidateMutations)
    }

    // mutates a seed KG with the mutation operators suitable for EL ontologies
    private fun elMutation() {
        // create selection of mutations that can be applied
        singleMutation(elReasonerMutations)
    }


    // apply one mutation to the input KG
    // argument: candidateMutations to apply
    private fun singleMutation(candidateMutations: List<KClass<out Mutation>>) {
        // get seed, mask and output files
        if (seed == null){
            println("You need to provide a seed knowledge graph")
            exitProcess(-1)
        } else if(!seed!!.exists()){
            println("File ${seed!!.path} does not exist")
            exitProcess(-1)
        }
        val seedKG =
            if (owlDocument)
                OwlFileHandler().loadOwlDocument(seed!!)
            else
                RDFDataMgr.loadDataset(seed!!.absolutePath).defaultModel

        val shapes: Shapes? = if(shaclMaskFile != null && !shaclMaskFile!!.exists()){
            println("File ${shaclMaskFile!!.path} does not exist")
            exitProcess(-1)
        } else if(shaclMaskFile != null) {
            val shapesGraph = RDFDataMgr.loadGraph(shaclMaskFile!!.absolutePath)
            Shapes.parse(shapesGraph)
        } else null

        val outputPath: File? = if(outputFile != null && outputFile!!.exists() && !overwriteOutput){
            println("Output file ${outputFile!!.path} does already exist. Please choose a different name or set \"--overwrite\" flag.")
            exitProcess(-1)
        } else if(outputFile == null) {
            println("Please provide an output file to save the mutated KG to.")
            exitProcess(-1)
        } else outputFile

        val mask = RobustnessMask(verbose, shapes)

        val ms = MutationSequence(verbose)
        // use random selection of a mutation. Select mutation operator based on seed for random selector, if provided
        // (use default generator otherwise)
        val generator = selectionSeed?.let { Random(it) } ?: randomGenerator

        // add mutation operators
        // if no number of mutations is provided --> apply one
        for (j in 1..(numberMutations ?: 1)) {
            val mutation = candidateMutations.random(generator)
            ms.addRandom(mutation)
        }

        // create mutator and apply mutation
        val m = Mutator(ms, verbose)
        val res = m.mutate(seedKG)

        // check if output directory exists and create it, if necessary
        Files.createDirectories(outputPath!!.parentFile.toPath())
        // safe result
        if (owlDocument)
            OwlFileHandler().saveOwlDocument(res, outputFile!!)
        else
            RDFDataMgr.write(outputPath.outputStream(), res, Lang.TTL)

        // print summary, if required
        if (printMutationSummary)
            println("mutation summary:\n" + m.getStringSummary())

    }


    private fun testMiniPipes() {
        //if(!seed.exists()) throw Exception("Input file $seed does not exist")
        //val input = RDFDataMgr.loadDataset(seed.absolutePath).defaultModel

        val input = RDFDataMgr.loadDataset("examples/miniPipes.ttl").defaultModel
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
        else
            println("Mutation Generator works as expected.")
    }

    private fun runSuaveGenerator() {
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
            val sg = SuaveTestCaseGenerator(verbose)

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

            val mask = RobustnessMask(verbose, shapes)

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

    private fun runGeoGenerator() {

        val maskFiles = listOf(
            "sut/geo/masks/mask0.ttl",
            "sut/geo/masks/mask1.ttl"
        )

        var id = 0
        for (maskFile in maskFiles) {
            val gg = GeoTestCaseGenerator(verbose)
            val numberOfMutants = 100
            val numberOfMutations = 2

            val shapes = parseShapes(File(maskFile))
            val mask = RobustnessMask(verbose, shapes)

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

    // evaluates provided shapes against the geo test runs
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
    private fun generateSuaveAttemptsGraph() {
        val numberOfMutants = sampleSize?:100
        val outputFile = File("sut/suave/evaluation/attemptsPerMask.csv")
        SuaveEvaluationGraphGenerator(false).generateGraph(numberOfMutants, outputFile)
    }

    // generates graph for journal extension
    private fun generateElCoverageGraphs() {
        val coverageGraphGenerator = if (sampleSize != null)
            CoverageEvaluationGraphGenerator(sampleSize!!)
        else
            CoverageEvaluationGraphGenerator()

        // EL coverage graph
        val inputDirectoryEL = File("sut/reasoners/ontologies_ore")
        val outputFileEl = File("sut/reasoners/evaluation/inputCoverageEL.csv")
        val owlAnalyzer = OwlOntologyAnalyzer()
        coverageGraphGenerator.analyzeInputCoverage(inputDirectoryEL, elReasonerMutations, outputFileEl, owlAnalyzer)
    }
    private fun generateSuaveCoverageGraphs() {
        val coverageGraphGenerator = if (sampleSize != null)
            CoverageEvaluationGraphGenerator(sampleSize!!)
        else
            CoverageEvaluationGraphGenerator()

        // suave coverage graph
        val outputFileSuave = File("sut/suave/evaluation/inputCoverageSuave.csv")
        val suaveAnalyzer = SuaveOntologyAnalyzer()
        coverageGraphGenerator.analyzeSuaveInputCoverage(outputFileSuave,suaveAnalyzer)

    }

    // analyze, how well minimization worked
    private fun analyzeMinimization() {
        val inputFiles = listOf(
            Pair(
                File("sut/reasoners/foundBugs/P2/ont_5.owl"),
                File("sut/reasoners/foundBugs/P2/ont_5.owl.minimal.owl")
            ),
            Pair(
                File("sut/reasoners/foundBugs/P4/ont_1709.owl"),
                File("sut/reasoners/foundBugs/P4/ont_1709.owl.minimal.owl")
            ),
            Pair(
                File("sut/reasoners/foundBugs/P5/ont_35.owl"),
                File("sut/reasoners/foundBugs/P5/ont_35.owl.minimal.manual.owl")
            ),
            Pair(
                File("sut/reasoners/foundBugs/H1/ont_107.owl"),
                File("sut/reasoners/foundBugs/H1/minimal.owl")
            )
        )

        for ((original, minimal) in inputFiles) {
            val originalSize = OwlFileHandler().loadOwlDocument(original).listStatements().toSet().size.toDouble()
            val minimalSize = OwlFileHandler().loadOwlDocument(minimal).listStatements().toSet().size.toDouble()
            val reduction = (originalSize - minimalSize) / originalSize
            println("minimized ${original.path}")
            println("original size: $originalSize")
            println("minimized size: $minimalSize")
            println("triples removed: ${originalSize-minimalSize}")
            println("reduction: $reduction")

        }
    }

    private fun listSuaveFeatures() {
        val suavePath = "sut/suave/suave_ontologies/suave_original_with_imports.ttl"
        val suaveWithImports = RDFDataMgr.loadDataset(suavePath).defaultModel

        val features = SuaveOntologyAnalyzer().getFeatures(suaveWithImports)

        println(features)
        println("number of feauters: ${features.size}")

        val suavePathUnmutated = "sut/suave/suave_ontologies/suave_original_with_imports_unmutatable.owl"
        val suaveWithImportsUnmutated = RDFDataMgr.loadDataset(suavePathUnmutated).defaultModel

        val featuresUnmutated = SuaveOntologyAnalyzer().getFeatures(suaveWithImportsUnmutated)
        println("number of feauters unmutatable part: ${featuresUnmutated.size}")


    }
}


fun main(args: Array<String>) = Main().main(args)