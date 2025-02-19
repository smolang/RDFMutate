package org.smolang.robust.domainSpecific.reasoner
import kotlinx.coroutines.*

import org.apache.jena.rdf.model.Model
import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.randomGenerator
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass

// class to produce evaluation graph for EL reasoners
class OwlEvaluationGraphGenerator(private val sampleSize : Int =100 ) {
    private val ontologyAnalyzer = OntologyAnalyzer()
    private val owlFileHandler = OwlFileHandler()

    val verbose = false
    // maps numbers of mutation operators to rest
    //private val mutationNumbers = listOf(200)
    private val mutationNumbers = listOf(0,1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100)


    private fun allOwlFiles(directory: File) : List<File> {
        // filter for files that end with ".owl"
        return Files.walk(directory.toPath())
            .filter { path -> path.toString().endsWith(".owl") }
            .toList()
            .map { path -> path.toFile() }
    }

    fun analyzeElInputCoverage(inputDirectory: File,
                                       mutationOperators :  List<KClass<out Mutation>>,
                                       outputFile: File) = runBlocking{

        val inputFiles = allOwlFiles(inputDirectory)  // sample

        // initialize map with results
        val results : MutableMap<Int, MutableList<Set<Int>>> = mutableMapOf()
        for (mutCount in mutationNumbers)
            results[mutCount] = mutableListOf()

        val totalTests = sampleSize * mutationNumbers.size

        // iterate over all selected files in directory

        var count = 1
        for (mutCount in mutationNumbers) {
            for (sampleID in 1..sampleSize) {
                println("Progress: $count/$totalTests")

                var res : Model? = null
                while (res == null) {

                    val inputFile = inputFiles.random(randomGenerator)
                    // load ontology
                    val seedOntology = owlFileHandler.loadOwlDocument(inputFile)

                    // use timeout of 10s
                    res = timedMutation2(seedOntology, mutationOperators, mutCount, 30000)

                    //res = timedMutation3(inputFile, mutCount, 5000)

                }


                // safe result
                results[mutCount]?.add(ontologyAnalyzer.getOwlFeaturesHashed(res))
                count += 1
            }
        }

        // compute results based on cumulative coverage of 10 test cases
        val results10 = cumulativeResult(results, 10)
        val results100 = cumulativeResult(results, 100)

        toCSV(results, results10, results100, outputFile)

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

    private fun timedMutation3(seedOntologyFile: File,
                               mutCount : Int,
                               timeout: Long ): Model? {
        var result: Model? = null
        val seedOntology = seedOntologyFile.absolutePath
        val mutantOntology =File("./sut/reasoners/evaluation/temp.owl")

        val command = "java -jar ./build/libs/OntoMutate-0.1.jar " +
                "--el-mutate " +
                "--seedKG=$seedOntology " +
                "--num_mut=$mutCount " +
                "--selection_seed=2 " +
                "--owl " +
                "--overwrite " +
                "--print-summary " +
                "--out=$mutantOntology "

        val mutationProcess = Runtime.getRuntime().exec(command)
        val finished = mutationProcess.waitFor(timeout, TimeUnit.MILLISECONDS)
        if (!finished) {
            if (verbose) println("mutator did not finish in time.")
            return null
        }

        result = owlFileHandler.loadOwlDocument(mutantOntology)


        return result
    }


    // saves results to .csv file
    private fun toCSV(results : MutableMap<Int, MutableList<Set<Int>>>,
                     results10 : MutableMap<Int, MutableList<Set<Int>>>,
                      results100 : MutableMap<Int, MutableList<Set<Int>>>,
                      outputFile : File) {
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
        countCombine : Int) : MutableMap<Int, MutableList<Set<T>>> {

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