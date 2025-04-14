package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.Resource

// an atom representing a fresh node that is introduced
class FreshNodeAtom(val variable: Resource) : MutationAtom() {

   companion object {
       // iri that is used in SWRL rules to mark atoms that declare fresh nodes
       const val BUILTIN_IRI = "${MUTATE_PREFIX}newNode"
       // prefix that is used for naming the newly added nodes
       const val NAME_PREFIX = "${MUTATE_PREFIX}newNode-"
   }

    override fun toLocalString(): String {
        return "newNode(${variable.localName})"
    }

    override fun containsResource(r: Resource): Boolean {
        return (r == variable)
    }
}