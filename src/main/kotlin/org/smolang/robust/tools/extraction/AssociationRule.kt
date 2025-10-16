package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.smolang.robust.tools.ruleMutations.MutationAtom

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
}