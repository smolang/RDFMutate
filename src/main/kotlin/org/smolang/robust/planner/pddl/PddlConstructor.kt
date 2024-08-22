package org.smolang.robust.planner.pddl

// class to bundle some conversions of objects to pddl syntax
class PddlConstructor {
    companion object {

        // turns relation into predicate declaration
        fun relationToPredDeclaration(relation: String, arity : Int) : String {
            var arguments = ""
            for (i in 1..arity)
                arguments += "?x$i "
            return "($relation $arguments)"
        }
    }
}