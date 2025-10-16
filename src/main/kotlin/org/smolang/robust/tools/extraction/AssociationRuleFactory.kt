package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.Resource
import org.smolang.robust.tools.ruleMutations.MutationAtomFactory

// factory to create association rules
// optional argument: a mapping from prefixes to long IRIs
class AssociationRuleFactory(val prefixMap: Map<String, String> = mapOf()) {
    val atomFactory = MutationAtomFactory(prefixMap)

    // produces association rule from mined string representation
    fun getAssociationRule(rule: String): AssociationRule {
        val variables = AssociationRule.getVariables(rule) // extract variables from mined rule
        return getAssociationRule(rule, variables)
    }

    // creates association rule from mined string representation and variable mapping
    fun getAssociationRule(rule: String, variables: Map<String, Resource>): AssociationRule {
        val implication = "->".toRegex()

        // there should be exactly one arrow in the rule
        assert(
            implication.findAll(rule).toSet().size == 1
        ) { "rule patterns need to contain exactly one implication arrow \"->\"" }
        val split = rule.split("->")
        val body = split[0]
        val head = split[1]

        val atom = "\\(([^)]*)\\)".toRegex()
        val bodyAtomsStrings = atom.findAll(body).map{ r -> r.value}.toList()
        val headAtomString = atom.findAll(head).map{r -> r.value}.toList().single()

        //println("body atoms: $bodyAtoms")
        //println("head atom: $headAtom")

        val bodyAtoms =  bodyAtomsStrings.map { a ->
            atomFactory.getAtom(a, variables)
        }
        val headAtom = atomFactory.getAtom(headAtomString, variables)
        return AssociationRule(bodyAtoms, headAtom, rule)
    }

}