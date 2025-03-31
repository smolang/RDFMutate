package org.smolang.robust.tools

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.smolang.robust.mainLogger

// interface to represent atoms (assertions) that appear in head and body of mutations
abstract class MutationAtom {

     // returns atom as used in SPARQL query; variables are mapped
     abstract  fun toSparqlString(rdf2sparql : Map<RDFNode, String>) : String?

     // a string representation using local names for resources
     abstract fun toLocalString() : String

     abstract fun containsResource(r : Resource) : Boolean

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

