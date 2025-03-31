package org.smolang.robust.tools

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.containsResource
import org.smolang.robust.mainLogger
import org.smolang.robust.toLocalString

abstract class StatementAtom(val statement: Statement)  : MutationAtom() {

    override fun toSparqlString(rdf2sparql: Map<RDFNode, String>): String? {
        val sub = nodeToSparqlString(statement.subject, rdf2sparql)
        val pred = nodeToSparqlString(statement.predicate, rdf2sparql)
        val obj = nodeToSparqlString(statement.`object`, rdf2sparql)

        // check, if one got valid results
        if (sub == null || pred == null || obj == null) {
            mainLogger.error("Could not transform statement $statement into SPARQL.")
            return null
        }

        return "$sub $pred $obj."

    }

    override fun containsResource(r: Resource): Boolean {
        return statement.containsResource(r)
    }
}