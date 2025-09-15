package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.tools.containsResource

abstract class StatementAtom(val statement: Statement)  : MutationAtom(), SparqlMutationAtom{

    override fun toSparqlString(rdf2sparql: Map<RDFNode, String>): String? {
        val sub = nodeToSparqlString(statement.subject, rdf2sparql)
        val pred = nodeToSparqlString(statement.predicate, rdf2sparql)
        val obj = nodeToSparqlString(statement.`object`, rdf2sparql)

        // check, if one got valid results
        if (sub == null || pred == null || obj == null) {
            println("ERROR: Could not transform statement $statement into SPARQL.")
            return null
        }

        return "$sub $pred $obj."

    }

    override fun containsResource(r: Resource): Boolean {
        return statement.containsResource(r)
    }
}