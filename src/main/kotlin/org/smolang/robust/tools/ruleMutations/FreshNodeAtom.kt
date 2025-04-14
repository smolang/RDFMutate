package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.Resource

// an atom representing a fresh node that is introduced
class FreshNodeAtom(val variable: Resource) : MutationAtom() {

   companion object {
       // iri that is used in SWRL rules to mark atoms that declare fresh nodes
       val BUILTIN_IRI = "https://smolang.org/rdfMutate#newNode"
       // prefix that is used for naming the newly added nodes
       val NAME_PREFIX = "https://smolang.org/rdfMutate#newNode-"
   }

    override fun toLocalString(): String {
        val nodeName = if (variable.isResource) {
            variable.asResource().localName
        }
        else
            variable.toString()
        return "newNode($nodeName)"
    }

    override fun containsResource(r: Resource): Boolean {
        return (r == variable)
    }
}