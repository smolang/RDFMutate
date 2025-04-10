package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource

// an atom representing a fresh node that is introduced
class FreshNodeAtom(val node: RDFNode) : MutationAtom() {

   companion object {
       val IRI = "https://smolang.org/rdfMutate#newNode"
   }

    override fun toLocalString(): String {
        val nodeName = if (node.isResource) {
            node.asResource().localName
        }
        else
            node.toString()
        return "newNode($nodeName)"
    }

    override fun containsResource(r: Resource): Boolean {
        return (r == node)
    }
}