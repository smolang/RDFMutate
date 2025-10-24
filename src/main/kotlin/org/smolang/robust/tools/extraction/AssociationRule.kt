package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.tools.ruleMutations.FreshNodeAtom
import org.smolang.robust.tools.ruleMutations.MutationAtom
import org.smolang.robust.tools.ruleMutations.NegativeStatementAtom
import org.smolang.robust.tools.ruleMutations.PositiveStatementAtom

// represents a mined rule: list of atoms as body and a single atom as head
class AssociationRule(
    val bodyAtoms: List<MutationAtom>,
    val headAtom: MutationAtom,
    val minedString: String // string representation as mined from KGs
) {
    companion object {
        // extracts set of variables from string representing a rule
        // i.e., extract all elements starting with "?"
        // represent variables as mapping from strings to resources
        fun getVariables(s: String): Map<String, Resource> {
            val regex = "\\?.".toRegex()
            val stringVars= regex.findAll(s).map { it.value }.toSet()

            // map string representations of variables to Resources
            return stringVars.associateWith { s ->
                ResourceFactory.createResource(
                    "${MutationAtom.MUTATE_PREFIX}variable${s.removePrefix("?")}"//$varID"
                )
            }
        }
    }

    override fun toString(): String {
        val body = bodyAtoms.fold("") { s, a -> "$s,${a.toLocalString()}" }
        return "AssociationRule(body=$body, head=${headAtom.toLocalString()})"
    }

    // variables that occur inside rule
    val variables = getVariables(minedString)

    // extracts mutations from the
    // one rule results in multiple mutations, depending on how many of the body atoms match
    fun getAbstractMutations(): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        abstractMutations.addAll(getAddingOperators(variables))
        abstractMutations.addAll(getDeletingOperators(variables))

        return abstractMutations
    }

    // creates all the operators that only add new triples to graph
    private fun getAddingOperators(variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are newly added VS searched for in KG
        val newBodyAtomCombinations = subsets(bodyAtoms)
        //println(newBodyAtomCombinations)

        newBodyAtomCombinations.forEach { newBodyAtoms ->
            // build new head; add body atoms that are newly added
            val mutationHead = mutableListOf(headAtom)
            bodyAtoms.forEach { atom ->
                if (newBodyAtoms.contains(atom)) mutationHead.add(atom)
            }
            val headVariables = containedVars(mutationHead, variables)

            // identify elements that remain in body
            val mutationBody = bodyAtoms.filter { atom -> !newBodyAtoms.contains(atom) }.toMutableList()
            val bodyVariables = containedVars(mutationBody, variables)

            // add declarations as new nodes for variables that do not occur in body anymore but in head
            val freshNodes = headVariables.minus(bodyVariables)
            //println(freshNodes)
            freshNodes.forEach { freshNode ->
                mutationBody.add(FreshNodeAtom(freshNode))
            }

            // assert that body covers at least all head variables
            val finalBodyVariables = containedVars(mutationBody, variables)
            assert(headVariables.minus(finalBodyVariables).isEmpty()) {"ERROR: something went wrong"}


            // build configuration
            val config = RuleMutationConfiguration(
                mutationBody,
                mutationHead,
                finalBodyVariables,
                headVariables
            )

            // build abstract mutation and add to list
            abstractMutations.add(AbstractMutation(RuleMutation::class, config))
        }
        return abstractMutations
    }

    // creates all the operators that only delete triples from the graph
    private fun getDeletingOperators(variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are deleted together with the head
        val deletedBodyAtomCombinations = subsets(bodyAtoms)

        // new body for mutation
        val mutationBody = bodyAtoms.toMutableList()
        mutationBody.add(headAtom)
        val bodyVariables = containedVars(mutationBody, variables)

        deletedBodyAtomCombinations.forEach { deletedBodyAtoms ->
            // only create mutation operator if set of selected body atoms is not empty
            val mutationHead = mutableListOf<MutationAtom>()
            deletedBodyAtoms.forEach { atom ->
                // add for each selected positive assertion a negative assertion to the head
                if (atom is PositiveStatementAtom) {
                    mutationHead.add(NegativeStatementAtom(atom.statement))
                }
            }
            if (!mutationHead.isEmpty() && headAtom is PositiveStatementAtom) {
                // only if we were able to invalidate the rule body by negating at least one positive statement
                // can we safely delete the head as well and define this as an abstract mutation
                mutationHead.add(NegativeStatementAtom(headAtom.statement))
                val headVariables = containedVars(mutationHead, variables)

                val config = RuleMutationConfiguration(
                    mutationBody,
                    mutationHead,
                    bodyVariables,
                    headVariables
                )
                // add abstract mutation
                abstractMutations.add(AbstractMutation(RuleMutation::class, config))
            }
        }
        return abstractMutations
    }

    // produces all subsets for provided set
    private fun <T> subsets(elements: List<T>): List<List<T>> {
        // add empty set
        if (elements.isEmpty())
            return listOf(listOf()) // return set with empty set

        // there is at least an element
        val head = elements.first()
        val rest = elements.filter { e -> e != head }

        // recursive call
        val recursiveSets = subsets(rest)

        val subsets: MutableList<List<T>> = mutableListOf()
        recursiveSets.forEach { set ->
            subsets.add(set)
            val extendedSet = mutableListOf<T>()
            set.forEach { e -> extendedSet.add(e)}
            extendedSet.add(head)
            subsets.add(extendedSet)
        }

        return  subsets
    }

    // returns the variables (as Resources) that are contained in a list of atoms
    private fun containedVars(atoms: List<MutationAtom>, variables: Map<String, Resource>): Set<Resource> {
        // collect all variables from all atoms
        return atoms.flatMap { atom ->
            variables.values.filter { variable -> atom.containsResource(variable) }
        }.toSet()
    }
}