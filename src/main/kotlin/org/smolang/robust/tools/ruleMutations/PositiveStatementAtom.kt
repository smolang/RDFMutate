package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Statement
import org.semanticweb.owlapi.model.SWRLAtom
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.toLocalString

class PositiveStatementAtom(statement: Statement, isDataValueProp: Boolean= false) : StatementAtom(statement, isDataValueProp) {
    override fun toLocalString(): String {
        return statement.toLocalString()
    }

    override fun asSWRLAtom(dataFactory: DataFactory, variables: Set<RDFNode>): SWRLAtom? {
        return when (isDataValueProp) {
            false -> asSWRLObjectPropertyAtom(dataFactory, variables)
            true -> asSWRLDataPropertyAtom(dataFactory, variables)
        }
    }

    fun asSWRLObjectPropertyAtom(dataFactory: DataFactory, variables: Set<RDFNode>): SWRLAtom? {
        val arg0 = asSWRLIArgument(statement.subject, variables, dataFactory)
        val arg1 = asSWRLIArgument(statement.`object`, variables, dataFactory)

        if (arg0 == null || arg1 == null) {
            mainLogger.warn("Could not create SWRL atom for atom $this.")
            return null
        }
        return dataFactory.getSWRLObjectPropertyAtom(
            dataFactory.getOWLObjectProperty(statement.predicate.uri),
            arg0,
            arg1
        )
    }

    fun asSWRLDataPropertyAtom(dataFactory: DataFactory, variables: Set<RDFNode>): SWRLAtom? {
        val arg0 = asSWRLIArgument(statement.subject, variables, dataFactory)
        val arg1 = asSWRLDArgument(statement.`object`, variables, dataFactory)

        if (arg0 == null) {
            mainLogger.warn("Could not create SWRL atom for atom $this.")
            return null
        }
        return dataFactory.getSWRLDataPropertyAtom(
            dataFactory.getOWLDataProperty(statement.predicate.uri),
            arg0,
            arg1
        )
    }
}