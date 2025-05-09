package org.smolang.robust.domainSpecific.reasoner

import org.apache.jena.rdf.model.Model
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.operators.AddClassAssertionMutation
import org.smolang.robust.mutant.operators.AddDataHasValueMutation
import org.smolang.robust.mutant.operators.AddDataPropDomainMutation
import org.smolang.robust.mutant.operators.AddDataPropRangeMutation
import org.smolang.robust.mutant.operators.AddDatatypeDefinition
import org.smolang.robust.mutant.operators.AddDifferentIndividualAssertionMutation
import org.smolang.robust.mutant.operators.AddDisjointClassRelationMutation
import org.smolang.robust.mutant.operators.AddELDataIntersectionOfMutation
import org.smolang.robust.mutant.operators.AddELDataOneOfMutation
import org.smolang.robust.mutant.operators.AddELObjectOneOfMutation
import org.smolang.robust.mutant.operators.AddELSimpleDataSomeValuesFromMutation
import org.smolang.robust.mutant.operators.AddEquivDataPropMutation
import org.smolang.robust.mutant.operators.AddEquivObjectPropMutation
import org.smolang.robust.mutant.operators.AddEquivalentClassRelationMutation
import org.smolang.robust.mutant.operators.AddHasKeyMutation
import org.smolang.robust.mutant.operators.AddIndividualMutation
import org.smolang.robust.mutant.operators.AddNegativeDataPropertyRelationMutation
import org.smolang.robust.mutant.operators.AddNegativeObjectPropertyRelationMutation
import org.smolang.robust.mutant.operators.AddObjectHasSelfMutation
import org.smolang.robust.mutant.operators.AddObjectHasValueMutation
import org.smolang.robust.mutant.operators.AddObjectIntersectionOfMutation
import org.smolang.robust.mutant.operators.AddObjectPropDomainMutation
import org.smolang.robust.mutant.operators.AddObjectPropRangeMutation
import org.smolang.robust.mutant.operators.AddObjectPropertyRelationMutation
import org.smolang.robust.mutant.operators.AddObjectSomeValuesFromMutation
import org.smolang.robust.mutant.operators.AddPropertyChainMutation
import org.smolang.robust.mutant.operators.AddReflexiveObjectPropertyRelationMutation
import org.smolang.robust.mutant.operators.AddSameIndividualAssertionMutation
import org.smolang.robust.mutant.operators.AddSubDataPropMutation
import org.smolang.robust.mutant.operators.AddSubObjectPropMutation
import org.smolang.robust.mutant.operators.AddSubclassRelationMutation
import org.smolang.robust.mutant.operators.AddTransitiveObjectPropertyRelationMutation
import org.smolang.robust.mutant.operators.BasicAddDataPropertyRelationMutation
import org.smolang.robust.mutant.operators.CEUAMutation
import org.smolang.robust.mutant.operators.DeclareClassMutation
import org.smolang.robust.mutant.operators.DeclareDataPropMutation
import org.smolang.robust.mutant.operators.DeclareObjectPropMutation
import org.smolang.robust.mutant.operators.RemoveClassAssertionMutation
import org.smolang.robust.mutant.operators.RemoveDataPropertyRelationMutation
import org.smolang.robust.mutant.operators.RemoveDifferentIndividualAssertionMutation
import org.smolang.robust.mutant.operators.RemoveDisjointClassRelationMutation
import org.smolang.robust.mutant.operators.RemoveDomainRelationMutation
import org.smolang.robust.mutant.operators.RemoveEquivClassRelationMutation
import org.smolang.robust.mutant.operators.RemoveEquivPropMutation
import org.smolang.robust.mutant.operators.RemoveIndividualMutation
import org.smolang.robust.mutant.operators.RemoveNegativePropertyAssertionMutation
import org.smolang.robust.mutant.operators.RemoveObjectPropertyRelationMutation
import org.smolang.robust.mutant.operators.RemoveRangeRelationMutation
import org.smolang.robust.mutant.operators.RemoveSameIndividualAssertionMutation
import org.smolang.robust.mutant.operators.RemoveSubPropMutation
import org.smolang.robust.mutant.operators.RemoveSubclassRelationMutation
import org.smolang.robust.mutant.operators.ReplaceClassWithBottomMutation
import org.smolang.robust.mutant.operators.ReplaceClassWithSiblingMutation
import org.smolang.robust.mutant.operators.ReplaceClassWithTopMutation
import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.OwlOntologyInterface
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class ElGenerationTimeAnalyzer {

    private val owlOntologyInterface = OwlOntologyInterface()

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

    // generates data for relation: number of mutations vs. time
    fun timePerMutation(
        input: File,
        output: File,
        sampleSize: Int = 10,   // elements per number of mutations
        mutationCounts: List<Int> = (0..100).toList(), // mutation numbers for which mutants are generated
        timeout: Long = 10000L // timeout in milliseconds
        ) {

        val results = mutableListOf<MutationResult>()

        // either collect ontologies or just chose file
        val inputFiles = if (input.isDirectory)
            allOwlFiles(input)  // sample
        else
            setOf(input)

        val totalTests = sampleSize * mutationCounts.size

        var count = 1
        for (mutCount in mutationCounts) {
            for (sampleID in 1..sampleSize) {
                mainLogger.info("Progress: $count/$totalTests")
                var res : MutationResult? = null
                while (res == null) {
                    val inputFile = inputFiles.random(randomGenerator)
                    // load ontology
                    val seedOntology = owlOntologyInterface.loadOwlDocument(inputFile)
                    // use timeout of 30s
                    res = timedMutation(seedOntology, elReasonerMutations, mutCount, timeout)

                    if (res == null) {
                        // timeout occurred --> save timeout*10 as time
                        res = MutationResult(
                            seedOntology.listStatements().toList().size,
                            mutCount,
                            timeout*10
                        )
                    }
                }

                // safe result
                results.add(res)
                count += 1
            }
        }
        // export results
        saveToCSV(output, results)
    }

    // return mutant or null
    private fun timedMutation(seedOntology: Model,
                               mutationOperators :  List<KClass<out Mutation>>,
                               mutCount : Int,
                               timeout: Long ): MutationResult? {
        var result : MutationResult? = null
        val t = thread {
            // stop time
            val startTime = System.currentTimeMillis()
            // collect as many mutations as necessary
            var current = seedOntology
            var i = 0
            while (i < mutCount ) {
                val ms = MutationSequence()
                ms.addRandom(mutationOperators.random(randomGenerator))

                // apply mutations
                val m = Mutator(ms)
                if (!Thread.currentThread().isInterrupted)
                    current = m.mutate(current)
                i += 1
            }
            if (!Thread.currentThread().isInterrupted)
                result = null

            // stop total time
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime

            if (totalTime < timeout) {
                // collect information for result
                result = MutationResult(
                    seedOntology.listStatements().toList().size,
                    mutCount,
                    totalTime
                )
            }

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

    private fun allOwlFiles(directory: File) : List<File> {
        // filter for files that end with ".owl"
        return Files.walk(directory.toPath())
            .filter { path -> path.toString().endsWith(".owl") }
            .toList()
            .map { path -> path.toFile() }
    }

    private fun saveToCSV(
        output: File,
        results: List<MutationResult>
    ) {
        // output results to csv file
        FileOutputStream(output).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("seedSize,numMutations,computationTime")
            writer.newLine()
            for (result in results) {
                val size = result.numberOfTriplesSeed
                val numMutations = result.numberOfMutations
                val time = result.computationTime

                writer.write("$size,$numMutations,$time")
                writer.newLine()
            }
            writer.close()
            mainLogger.info("export results to File $output")
        }
    }
}

// stores result of a mutation
data class MutationResult(
    val numberOfTriplesSeed: Int,
    val numberOfMutations: Int,
    val computationTime: Long // in ms
)