package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.Resource
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.tools.MutationFileParser
import org.smolang.robust.tools.extraction.AssociationRule
import org.smolang.robust.tools.extraction.AssociationRuleFactory
import java.io.File

// parses association rules to create abstract mutation operators
class AssociationRuleParser(val rulesFile: File): MutationFileParser() {

    // loads all rules form one file and creates abstract mutations for them
    override fun getAllAbstractMutations(): List<AbstractMutation> {
        val mutationOperators: List<AbstractMutation> = rulesFile.useLines { lines ->
            lines.flatMap {line ->
                //println("parse line \"$line\"")
                ruleToAbstractMutations(
                    AssociationRuleFactory().getAssociationRule(line)
                )
            }.toList()
        }
        return mutationOperators
    }

    // parses one rule into a configuration
    // one rule results in multiple configurations, depending on how many of the body atoms match
    fun ruleToAbstractMutations(rule: AssociationRule): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        abstractMutations.addAll(getAddingOperators(rule, rule.variables))
        abstractMutations.addAll(getDeletingOperators(rule, rule.variables))

        return abstractMutations
    }

    // creates all the operators that only add new triples to graph
    private fun getAddingOperators(rule: AssociationRule, variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are newly added VS searched for in KG
        val newBodyAtomCombinations = subsets(rule.bodyAtoms)
        //println(newBodyAtomCombinations)

        newBodyAtomCombinations.forEach { newBodyAtoms ->
            // build new head; add body atoms that are newly added
            val mutationHead = mutableListOf(rule.headAtom)
            rule.bodyAtoms.forEach { atom ->
                if (newBodyAtoms.contains(atom)) mutationHead.add(atom)
            }
            val headVariables = containedVars(mutationHead, variables)

            // identify elements that remain in body
            val mutationBody = rule.bodyAtoms.filter { atom -> !newBodyAtoms.contains(atom) }.toMutableList()
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
    private fun getDeletingOperators(rule: AssociationRule, variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are deleted together with the head
        val deletedBodyAtomCombinations = subsets(rule.bodyAtoms)

        // new body for mutation
        val mutationBody = rule.bodyAtoms.toMutableList()
        mutationBody.add(rule.headAtom)
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
            if (!mutationHead.isEmpty() && rule.headAtom is PositiveStatementAtom) {
                // only if we were able to invalidate the rule body by negating at least one positive statement
                // can we safely delete the head as well and define this as an abstract mutation
                mutationHead.add(NegativeStatementAtom(rule.headAtom.statement))
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