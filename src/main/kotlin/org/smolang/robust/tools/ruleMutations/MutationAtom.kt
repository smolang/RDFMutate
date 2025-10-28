package org.smolang.robust.tools.ruleMutations

import com.github.owlcs.ontapi.DataFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.semanticweb.owlapi.model.SWRLAtom
import org.semanticweb.owlapi.model.SWRLDArgument
import org.semanticweb.owlapi.model.SWRLIArgument
import org.smolang.robust.mainLogger

// interface to represent atoms (assertions) that appear in head and body of mutations
abstract class MutationAtom {
     companion object {
          // prefix that is used for special built-ins
          const val MUTATE_PREFIX = "https://smolang.org/rdfMutate#"
     }

     // a string representation using local names for resources
     abstract fun toLocalString() : String

     abstract fun containsResource(r : Resource) : Boolean

     abstract fun asSWRLAtom(dataFactory: DataFactory, variables: Set<RDFNode>) : SWRLAtom?

     // returns the node as swrl literal, i.e. either literal or as variable
     protected fun asSWRLDArgument(node: RDFNode, variables: Set<RDFNode>, dataFactory: DataFactory): SWRLDArgument {
          if (variables.contains(node))
               return dataFactory.getSWRLVariable(node.toString())

          // iri is not a variable
          return dataFactory.getSWRLLiteralArgument(dataFactory.getOWLLiteral(node.toString()))
     }

     // returns the node as swrl literal, i.e. either literal or as variable
     protected fun asSWRLIArgument(node: RDFNode, variables: Set<RDFNode>, dataFactory: DataFactory): SWRLIArgument? {
          if (variables.contains(node))
               return dataFactory.getSWRLVariable(node.toString())

          if (!node.isResource) {
               mainLogger.error("Can not save rule to SWRL atom because argument $node is not a resource.")
               return null
          }

          // iri is not a variable
          return dataFactory.getSWRLIndividualArgument(
               dataFactory.getOWLNamedIndividual(node.asResource().uri)
          )
     }
}

