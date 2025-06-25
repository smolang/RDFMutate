package org.smolang.robust.domainSpecific.reasoner

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.domainSpecific.KgAnalyzer
import org.smolang.robust.domainSpecific.suave.SuaveOntologyAnalyzer
import org.smolang.robust.domainSpecific.suave.SuaveTestCaseGenerator
import org.smolang.robust.mutant.EmptyMask
import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.randomGenerator
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass

// class to produce coverage graphs
class CoverageEvaluationGraphGenerator(private val sampleSize : Int =100 ) {
    private val owlFileHandler = OwlFileHandler()

    val verbose = false
    // maps numbers of mutation operators to rest
    //private val mutationNumbers = listOf(200)
    private val mutationNumbersEL = listOf(0,1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100)
    private val mutationNumbersSuave = listOf(0,1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100)



    private fun allOwlFiles(directory: File) : List<File> {
        // filter for files that end with ".owl"
        return Files.walk(directory.toPath())
            .filter { path -> path.toString().endsWith(".owl") }
            .toList()
            .map { path -> path.toFile() }
    }

    fun analyzeInputCoverage(input: File,
                             mutationOperators :  List<KClass<out Mutation>>,
                             outputFile: File,
                             analyzer: KgAnalyzer) {

        // either collect ontologies or just chose file
        val inputFiles = if (input.isDirectory)
            allOwlFiles(input)  // sample
        else
            setOf(input)

        // initialize map with results
        val results : MutableMap<Int, MutableList<Set<Int>>> = mutableMapOf()
        for (mutCount in mutationNumbersEL)
            results[mutCount] = mutableListOf()

        val totalTests = sampleSize * mutationNumbersEL.size

        // iterate over all selected files in directory

        var count = 1
        for (mutCount in mutationNumbersEL) {
            for (sampleID in 1..sampleSize) {
                println("Progress: $count/$totalTests")
                var res : Model? = null
                while (res == null) {
                    val inputFile = inputFiles.random(randomGenerator)
                    // load ontology
                    val seedOntology = owlFileHandler.loadOwlDocument(inputFile)
                    // use timeout of 30s
                    res = timedMutation2(seedOntology, mutationOperators, mutCount, 30000)
                }

                // safe result
                results[mutCount]?.add(analyzer.getFeaturesHashed(res))
                count += 1
            }
        }

        // compute results based on cumulative coverage of 10 test cases
        val results10 = cumulativeResult(results, 10, mutationNumbersEL)
        val results100 = cumulativeResult(results, 100, mutationNumbersEL)

        toCSV(results, results10, results100, outputFile, mutationNumbersEL)

    }


        // return mutant or null
    private fun timedMutation2(seedOntology: Model,
                                      mutationOperators :  List<KClass<out Mutation>>,
                                      mutCount : Int,
                                      timeout: Long ): Model? {
        var result : Model? = null
        val t = thread {
            // collect as many mutations as necessary
            var current = seedOntology
            var i = 0
            while (i < mutCount ) {
                val ms = MutationSequence(verbose)
                ms.addRandom(mutationOperators.random(randomGenerator))

                // apply mutations
                val m = Mutator(ms, verbose)
                if (!Thread.currentThread().isInterrupted)
                    current = m.mutate(current)
                i += 1
            }
            if (!Thread.currentThread().isInterrupted)
                result = current
        }
        var time = 0L
        val increment = 50L
        while (t.isAlive && time < timeout) {
            Thread.sleep(increment)
            time += increment
        }
        if (t.isAlive) {    // process did not finish in time --> kill it
            t.interrupt()
            Thread.sleep(1000L)
            while (t.isAlive) {
                t.stop()    // force thread to stop
                Thread.sleep(1000L)
            }
            return null
        }
        return result
    }

    fun analyzeSuaveInputCoverage(outputFile: File,
                                  analyzer: KgAnalyzer,
                                  baseline: Boolean) {

        // calculate features that are not saved to mutant due to how suave handles that stuff (but they still count
        // for the features
        val mrosPath = "sut/suave/suave_ontologies/mros_no_import.owl"
        val tomasysPath = "sut/suave/suave_ontologies/tomasys.owl"
        val mrosModel = RDFDataMgr.loadDataset(mrosPath).defaultModel!!
        val tomasysModel = RDFDataMgr.loadDataset(tomasysPath).defaultModel!!
        val featuresMros = SuaveOntologyAnalyzer().getFeaturesHashed(mrosModel)
        val featuresThomasys = SuaveOntologyAnalyzer().getFeaturesHashed(tomasysModel)

        val externalFeatures = featuresMros.plus(featuresThomasys)

        println("generate coverage graph for Suave")

        // initialize map with results
        val results : MutableMap<Int, MutableList<Set<Int>>> = mutableMapOf()
        for (mutCount in mutationNumbersSuave)
            results[mutCount] = mutableListOf()

        val totalTests = sampleSize * mutationNumbersSuave.size

        // iterate over all selected files in directory

        var count = 1
        for (mutCount in mutationNumbersSuave) {
            for (sampleID in 1..sampleSize) {
                println("Progress: $count/$totalTests")
                var res : Model? = null
                while (res == null) {
                    // generate suave mutation
                    res = suaveMutation(mutCount, baseline)
                }
                val features = analyzer.getFeaturesHashed(res).plus(externalFeatures)

                // safe result
                results[mutCount]?.add(features)
                count += 1
            }
        }

        // compute results based on cumulative coverage of 10 test cases
        //val results10 = cumulativeResult(results, 10)
       // val results100 = cumulativeResult(results, 100)
        val results10 = cumulativeResult(results, 10, mutationNumbersSuave)
        val results100 = cumulativeResult(results, 100, mutationNumbersSuave)

        toCSV(results, results10, results100, outputFile, mutationNumbersSuave)

    }

    private fun suaveMutation(numMutation : Int, baseline: Boolean = false) : Model {
        val mutantName = "tempCoverageAnalysisMutatant"

        val mutantFileName = "$mutantName.0.suave.owl"

        //val emptyMask = RobustnessMask(verbose, shacl = null)
        val emptyMask = EmptyMask(verbose)

        val sg = SuaveTestCaseGenerator(verbose)
        val rationDomainSpecific = if (baseline) 0.0 else 1.0
        // generate mutated ontology
        sg.generateSuaveMutants(
            numberMutants = 1,
            numMutation,
            rationDomainSpecific,
            useAddQAMutation = true,
            emptyMask,
            mutantName,
            saveMutants = true
        )

        // load saved ontology
        return RDFDataMgr.loadDataset("sut/suave/mutatedOnt/$mutantFileName").defaultModel
    }


    // saves results to .csv file
    private fun toCSV(results : MutableMap<Int, MutableList<Set<Int>>>,
                     results10 : MutableMap<Int, MutableList<Set<Int>>>,
                      results100 : MutableMap<Int, MutableList<Set<Int>>>,
                      outputFile : File,
                      mutationNumbers : List<Int>) {
        // output results to csv file
        FileOutputStream(outputFile).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("numMutations,avg,std,avg10,std10,avg100,std100")
            writer.newLine()
            for (mutCount in mutationNumbers) {
                val avg = results[mutCount]!!.map{it.size}.average()
                val std = results[mutCount]!!.map{it.size}.stdDev()
                val avg10 = results10[mutCount]!!.map{it.size}.average()
                val std10 = results10[mutCount]!!.map{it.size}.stdDev()
                val avg100 = results100[mutCount]!!.map{it.size}.average()
                val std100 = results100[mutCount]!!.map{it.size}.stdDev()
                writer.write("$mutCount,$avg,$std,$avg10,$std10,$avg100,$std100")
                writer.newLine()
            }
            writer.close()
            println("write to File $outputFile")
        }
    }

    // combines result of several mutants. Second argument: how many results to combine
    private fun <T>cumulativeResult(
        results : MutableMap<Int, MutableList<Set<T>>>,
        countCombine : Int,
        mutationNumbers : List<Int>) : MutableMap<Int, MutableList<Set<T>>> {

        val resultsCumulated : MutableMap<Int, MutableList<Set<T>>> = mutableMapOf()
        for (mutCount in mutationNumbers) {
            resultsCumulated[mutCount] = mutableListOf()
            // we use as many samples as for the other line
            for (i in 1..sampleSize) {
                val sample = randElements(results[mutCount]!!.toList(), countCombine)
                val set = sample.flatten().toSet()
                resultsCumulated[mutCount]!!.add(set)
            }
        }
        return resultsCumulated
    }

    // method to get a random sub-collection (possibly multiple occurrences)
    private fun <T>randElements(elements: List<T>, number: Int) : List<T> {
        val result = mutableListOf<T>()
        for (i in 1..number)
            result.add(elements.random(randomGenerator))

        return result
    }

    // methods to calculate standard deviation
    private fun Collection<Double>.stdDevDouble(): Double {
        val mean = this.sum() / this.size

        var standardDeviation = 0.0
        this.forEach { standardDeviation += (it - mean).pow(2.0) }

        return sqrt(standardDeviation / this.size)
    }

    fun Collection<Number>.stdDev(): Double {
        return this.map { it.toDouble() }.stdDevDouble()
    }
}