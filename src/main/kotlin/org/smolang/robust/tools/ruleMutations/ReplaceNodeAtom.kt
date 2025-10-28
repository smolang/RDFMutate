package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.SWRLAtom

// an atom that represents that "old" is replaced everywhere in the graph by "new"
class ReplaceNodeAtom(val old: RDFNode, val new: RDFNode) : MutationAtom() {
    companion object {
        // iri that is used in SWRL rules to mark atoms that declare replacement of nodes
        const val BUILTIN_IRI = "${MUTATE_PREFIX}replaceWith"
    }
    override fun toLocalString(): String {
        val oldName = if (old.isResource) {
            old.asResource().localName
        }
        else
            old.toString()

        val newName = if (new.isResource) {
            new.asResource().localName
        }
        else
            new.toString()

        return "replace($oldName, $newName)"
    }

    override fun containsResource(r: Resource): Boolean {
        return (r == old || r == new)
    }

    override fun asSWRLAtom(
        dataFactory: DataFactory,
        variables: Set<RDFNode>
    ): SWRLAtom? {
        return dataFactory.getSWRLBuiltInAtom(
            IRI.create(BUILTIN_IRI),
            listOf(
                asSWRLDArgument(old, variables, dataFactory),
                asSWRLDArgument(new, variables, dataFactory)
            )
        )
    }
}