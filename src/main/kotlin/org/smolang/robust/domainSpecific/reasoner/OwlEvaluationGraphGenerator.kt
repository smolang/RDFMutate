package org.smolang.robust.domainSpecific.reasoner

import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.randomGenerator
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.OwlOntologyInterface
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.reflect.KClass

// class to produce evaluation graph for EL reasoners
class OwlEvaluationGraphGenerator(
    private val mutationNumbers: List<Int> = listOf(0,1,2,3,4,5,6,7,8,9,10,20,30,40,50,75,100),
    private val sampleSize: Int = 100 // number of ontologies that are considered (might be more than in folder)
             ) {
    private val ontologyAnalyzer = OwlOntologyAnalyzer()
    private val owlOntologyInterface = OwlOntologyInterface()


    private fun allOwlFiles(directory: File) : List<File> {
        // filter for files that end with ".owl"
        return Files.walk(directory.toPath())
            .filter { path -> path.toString().endsWith(".owl") }
            .toList()
            .map { path -> path.toFile() }
    }

    fun analyzeElInputCoverage(inputDirectory: File,
                               mutationOperators :  List<KClass<out Mutation>>,
                               outputFile: File) {

        val inputFiles = randElements(allOwlFiles(inputDirectory), sampleSize)  // sample

        // initialize map with results
        val results : MutableMap<Int, MutableList<Set<Int>>> = mutableMapOf()
        for (mutCount in mutationNumbers)
            results[mutCount] = mutableListOf()

        val totalTests = sampleSize * mutationNumbers.size
        var count = 1


        // iterate over all files in directory
        for (inputFile in inputFiles) {
            // load ontology
            val seedOntology = owlOntologyInterface.loadOwlDocument(inputFile)

            for (mutCount in mutationNumbers) {
                mainLogger.info("Progress of analyzing input coverage: $count/$totalTests")

                // collect as many mutations as necessary
                val ms = MutationSequence()
                for (i in 1..mutCount)
                    ms.addRandom(mutationOperators.random(randomGenerator))

                // apply mutations
                val m = Mutator(ms)
                val res = m.mutate(seedOntology)

                // safe result
                results[mutCount]?.add(ontologyAnalyzer.getFeaturesHashed(res))
                count += 1
            }
        }

        // compute results based on cumulative coverage of 10 test cases
        val results10 = cumulativeResult(results, 10)
        val results100 = cumulativeResult(results, 100)

        toCSV(results, results10, results100, outputFile)


        /*

        for (mutCount in mutationNumbers) {
             println("$mutCount Average ${results[mutCount]!!.map{it.size}.average()}")
             println("$mutCount Std ${results[mutCount]!!.map{it.size}.stdDev()}")
         }

        for (mutCount in mutationNumbers) {
             println("$mutCount 10x Average ${results10[mutCount]!!.map{it.size}.average()}")
             println("$mutCount 10x Std ${results10[mutCount]!!.map{it.size}.stdDev()}")

             println("$mutCount 100x Average ${results100[mutCount]!!.map{it.size}.average()}")
             println("$mutCount 100x Std ${results100[mutCount]!!.map{it.size}.stdDev()}")
         }

         */
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
            mainLogger.info("write data for coverage graph to file $outputFile")
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

    // method to get a random sub-collection (possibly multiple occurences)
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