package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.SWRLAtom

// an atom that represents a node that will be deleted from the graph,
// i.e. all triples where it occurs are deleted
// be careful, this deletion might break the kg structure, e.g. lists
class DeleteNodeAtom(val node: RDFNode) : MutationAtom() {
    companion object {
        // iri that is used in SWRL rules to mark atoms that declare deletion of nodes
        const val BUILTIN_IRI = "${MUTATE_PREFIX}deleteNode"
    }

    override fun toLocalString(): String {
        val nodeName = if (node.isResource) {
            node.asResource().localName
        }
        else
            node.toString()
        return "deleteNode($nodeName)"
    }

    override fun containsResource(r: Resource): Boolean {
        return (r == node)
    }

    override fun asSWRLAtom(dataFactory: DataFactory, variables: Set<RDFNode>): SWRLAtom {
        return dataFactory.getSWRLBuiltInAtom(
            IRI.create(BUILTIN_IRI),
            listOf(
                asSWRLDArgument(node, variables, dataFactory),            )
        )
    }
}