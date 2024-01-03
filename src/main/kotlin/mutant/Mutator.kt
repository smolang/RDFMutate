package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.reasoner.ReasonerRegistry



class Mutator(private val mutSeq: MutationSequence, private val verbose: Boolean) {
    var globalMutation : Mutation? = null
    var ran = false

    // collects a string representation of all the applied mutations
    var appliedMutations : MutableList<String> = mutableListOf()

    fun mutate (seed : Model) : Model {
        ran = true
        appliedMutations = mutableListOf()
        globalMutation = Mutation(seed, verbose)
        var target = seed
        for (i  in 0 until mutSeq.size()) {
            val mutation = mutSeq[i].concreteMutation(target)
            //println("Mutation: $mutation")
            if(mutation.isApplicable()) {
                target = mutation.applyCopy()
                globalMutation?.mimicMutation(mutation)
                appliedMutations.add(mutation.toString())
            }
        }
        return target
    }

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

    fun validate(model: Model, contract : MutantContract) : Boolean{
        return contract.validate(model)
    }
}
