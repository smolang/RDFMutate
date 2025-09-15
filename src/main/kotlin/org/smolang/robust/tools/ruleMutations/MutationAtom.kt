package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource

// interface to represent atoms (assertions) that appear in head and body of mutations
abstract class MutationAtom {
     companion object {
          // prefix that is used for special built-ins
          const val MUTATE_PREFIX = "https://smolang.org/rdfMutate#"
     }

     // a string representation using local names for resources
     abstract fun toLocalString() : String

     abstract fun containsResource(r : Resource) : Boolean

}

