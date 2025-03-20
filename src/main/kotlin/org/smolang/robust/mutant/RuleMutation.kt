package org.smolang.robust.mutant

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Statement

// a mutation represented by a rule, i.e., SWRL rule
class RuleMutation {
    // condition
    val body : List<Statement> = listOf()
    // consequence
    val head : List<Statement> = listOf()
    // variables in the rule
    val bodyVariables : Set<RDFNode> = setOf()
    val headVariables : Set<RDFNode> = setOf()
    val variables : Set<RDFNode> get() = run { bodyVariables.union(headVariables) }


}