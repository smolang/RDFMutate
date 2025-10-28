package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.OWL
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.SWRLArgument
import org.semanticweb.owlapi.model.SWRLAtom
import org.smolang.robust.tools.toLocalString

class NegativeStatementAtom(statement: Statement) : StatementAtom(statement) {
    companion object {
        val BUILTIN_IRI = OWL.NegativePropertyAssertion.toString()
    }
    override fun toLocalString(): String {
        return "not${statement.toLocalString()}"
    }

    // create SWRL atom as built-in atom
    override fun asSWRLAtom(dataFactory: DataFactory, variables: Set<RDFNode>): SWRLAtom? {
        return dataFactory.getSWRLBuiltInAtom(
            IRI.create(BUILTIN_IRI),
            listOf(
                asSWRLDArgument(statement.subject, variables, dataFactory),
                asSWRLDArgument(statement.predicate, variables, dataFactory),
                asSWRLDArgument(statement.`object`, variables, dataFactory)
            )
        )
    }
}