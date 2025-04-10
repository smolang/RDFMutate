package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.smolang.robust.mainLogger

// a mutation atom that can be translated into SPARQL syntax
interface SparqlMutationAtom {
    // returns atom as used in SPARQL query; variables are mapped
     fun toSparqlString(rdf2sparql : Map<RDFNode, String>) : String?

    // returns the Sparql name of a node
    fun nodeToSparqlString(n : RDFNode, rdf2sparql : Map<RDFNode, String>) : String? {
        if (n.isLiteral)
            return  n.toString()

        if (!n.isResource) {
            mainLogger.warn("Encountered RDFNode $n while parsing of rule to mutation." +
                    "Depending on structure of node, this might not be supported and cause errors later.")
            return n.toString()
        }

        // check, if node is variable
        if (rdf2sparql.keys.contains(n.asResource())) {
            return rdf2sparql.getOrDefault(n.asResource(), null)
        }
        // n is resource but not a variable
        return "<${n.asResource()}>"
    }
}