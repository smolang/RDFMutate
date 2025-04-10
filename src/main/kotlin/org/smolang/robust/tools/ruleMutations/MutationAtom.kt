package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.smolang.robust.mainLogger

// interface to represent atoms (assertions) that appear in head and body of mutations
abstract class MutationAtom {

     // a string representation using local names for resources
     abstract fun toLocalString() : String

     abstract fun containsResource(r : Resource) : Boolean

}

