package org.smolang.robust.planner.pddl

class PddlDomain {
    private val types = mutableListOf<String>()
    private val predicates = mutableListOf<String>()
    private val actions = mutableListOf<PddlAction>()


    val usedObjects : Set<String> get() {
        return actions.flatMap { it.usedObjects }.toSet()
    }

    fun addType(type: String) {
        types.add(type)
    }

    fun addPredicate(predicate: String) {
        predicates.add(predicate)
    }

    fun addAction(action: PddlAction) {
        actions.add(action)
    }



    fun generate(domainName : String): String {
        val sb = StringBuilder()
        sb.appendLine("(define (domain $domainName)")
        sb.appendLine("  (:requirements :strips :typing :derived-predicates)")

        if (types.isNotEmpty()) {
            sb.appendLine("  (:types")
            types.forEach { sb.appendLine("    $it") }
            sb.appendLine("  )\n")
        }

        if (predicates.isNotEmpty()) {
            sb.appendLine("  (:predicates")
            predicates.sorted().forEach { sb.appendLine("    $it") }
            sb.appendLine("  )\n")
        }

        actions.forEach { action ->
            sb.appendLine(action.toString())
        }

        sb.appendLine(")")
        return sb.toString()
    }
}
