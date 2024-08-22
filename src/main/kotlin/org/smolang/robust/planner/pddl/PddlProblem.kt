package org.smolang.robust.planner.pddl

class PddlProblem() {
    private val objects = mutableSetOf<String>()
    private val initialState = mutableListOf<PddlAssertion>()
    private val goalState = mutableListOf<PddlAssertion>()

    // all assertions occuring in problem file
    private val allAssertions : List<PddlAssertion> get() {
        return  initialState.plus(goalState)
    }

    // all predicates occuring in the problem file
    val usedPredicates : Set<String> get() = run {
        allAssertions.map {
            PddlConstructor.relationToPredDeclaration(it.relation, it.arguments.size)
        }.toSet()
    }


    fun addObject(name: String, type: String) {
        objects.add("$name - $type")
    }

    fun addToInitialState(predicate: PddlAssertion) {
        initialState.add(predicate)
        predicate.arguments.forEach { addObject(it, "object") } // add objects that occur somewhere in initial state
    }

    fun addToGoalState(predicate: PddlAssertion) {
        goalState.add(predicate)
    }

    fun generate(domainName: String,
                 problemName: String): String {
        val sb = StringBuilder()
        sb.appendLine("(define (problem $problemName)")
        sb.appendLine("  (:domain $domainName)")

        if (objects.isNotEmpty()) {
            sb.appendLine("  (:objects")
            objects.sorted().forEach { sb.appendLine("    $it") }
            sb.appendLine("  )\n")
        }

        if (initialState.isNotEmpty()) {
            sb.appendLine("  (:init")
            initialState.sortedBy { it.toString() }.forEach { sb.appendLine("    $it") }
            sb.appendLine("  )\n")
        }

        if (goalState.isNotEmpty()) {
            sb.appendLine("  (:goal (and")
            goalState.sortedBy { it.toString() }.forEach { sb.appendLine("    $it") }
            sb.appendLine("  ))")
        }

        sb.appendLine(")")
        return sb.toString()
    }
}
