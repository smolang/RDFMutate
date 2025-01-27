package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import java.nio.file.Paths


class Mutator(private val mutSeq: MutationSequence, private val verbose: Boolean) {
    var globalMutation : Mutation? = null
    private var ran = false

    // collects a string representation of all the applied mutations
    var appliedMutations : MutableList<String> = mutableListOf()

    fun mutate (seed : Model) : Model {
        ran = true
        appliedMutations = mutableListOf()
        globalMutation = Mutation(seed, verbose)
        var target = seed
        for (i  in 0 until mutSeq.size()) {
            val mutation = mutSeq[i].concreteMutation(target)
            if (verbose)
                println("Mutation: $mutation")
            if(mutation.isApplicable()) {
                target = mutation.applyCopy()
                globalMutation?.mimicMutation(mutation)
                appliedMutations.add(mutation.toString())
            }
        }
        return target
    }

    // collect all nodes that are mentioned in the mutations
    val affectedNodes : Set<Resource>
        get() {
            val nodes: MutableSet<Resource> = mutableSetOf()
            // collect all nodes that are mentioned in the mutations
            for (add in globalMutation?.addSet ?: mutableSetOf()) {
                nodes.add(add.subject)
                nodes.add(add.predicate.asResource())
                if (add.`object`.isResource)
                    nodes.add(add.`object`.asResource())
            }
            for (add in globalMutation?.removeSet ?: mutableSetOf()) {
                nodes.add(add.subject)
                nodes.add(add.predicate.asResource())
                if (add.`object`.isResource)
                    nodes.add(add.`object`.asResource())
            }
            // collect all nodes that are mentioned in the mutations
            return nodes.toSet()
        }


    val addSet : Set<Statement>
        get() {
            return globalMutation?.addSet?.toSet() ?: setOf()
        }

    val removeSet : Set<Statement>
        get() {
            return globalMutation?.removeSet?.toSet() ?: setOf()
        }

    val numMutations : Int
        get() {
            return mutSeq.size()
        }

    val affectedSeedNodes : Set<Resource>
        get() = affectedNodes.intersect((globalMutation?.allNodes() ?: mutableSetOf()).toSet())

    fun validate(model: Model, contract : RobustnessMask) : Boolean{
        return contract.validate(model)
    }

    fun getStringSummary() : String {
        val numMut = numMutations
        val numDel = globalMutation?.removeSet?.size ?: -1
        val numAdd = globalMutation?.addSet?.size ?: -1
        val appliedMutations = appliedMutations
        val affectedSeedNodes = affectedSeedNodes.joinToString(",", "[", "]") {
            if (it.localName != null)
                it.localName
            else
                it.toString()
        }

        val addedAxioms = addSet.joinToString( ",", "[", "]")
        val removedAxioms = removeSet.joinToString( ",", "[", "]").replace("\n", ",")

        // first line: explanation; second line: values
        val result = "numMutations;numDel;numAdd;appliedMutations;affectedSeedNodes;addedAxioms;removedAxioms\n" +
                "$numMut;$numDel;$numAdd;$appliedMutations;$affectedSeedNodes;$addedAxioms;$removedAxioms"
        return result
    }
}

open class MutatorFactory(val verbose: Boolean) {

    open fun randomMutator() : Mutator {
        return Mutator(MutationSequence(verbose), verbose)
    }
}