package org.smolang.robust.planner.pddl

import org.apache.jena.rdf.model.Statement

class PddlAssertion(
    val relation : String,
    val arguments: List<String>
) {
    override fun toString() : String {
        return "($relation ${arguments.joinToString(" " )})"
    }
}