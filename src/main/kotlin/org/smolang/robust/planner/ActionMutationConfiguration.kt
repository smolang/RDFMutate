package org.smolang.robust.planner

import org.apache.jena.rdf.model.*
import org.smolang.robust.mutant.MutationConfiguration
import org.smolang.robust.planner.pddl.PddlAction


// configuration for planning action mutations
// they can be exported to planning actions

// parameters:
// triples to find (will be deleted)
// new triples that will be added
// resources that represent variables
// map to represent mapping from KG to PDDL
class ActionMutationConfiguration(
    val match : Set<Statement>,
    val replace : Set<Statement>,
    val variables : List<RDFNode>,
    private val mapToPddl : KgPddlMap = KgPddlMap(),
    private val mf : Model = ModelFactory.createDefaultModel()!!
) : MutationConfiguration() {

    init {
        // add elements if necessary
        for (s in match)
            mapToPddl.putAllElmentsIfAbsent(s)
        for (s in replace)
            mapToPddl.putAllElmentsIfAbsent(s)

        // add mapping for variables
        for (v in variables) {
            mapToPddl.addVariable(v)
        }
    }

    fun asPddlAction(name : String) : PddlAction {
        val parameters = variables.mapNotNull { mapToPddl.toPddl(it) }
        val preconditions = match.mapNotNull { mapToPddl.toPddl(it) }
        val effects = replace.mapNotNull { mapToPddl.toPddl(it) }

        return  PddlAction(
            name,
            parameters,
            preconditions,
            effects
        )
    }

    fun mapVariables(mapping : Map<RDFNode, RDFNode>) : ActionMutationConfiguration? {
        if (variables.minus(mapping.keys).isNotEmpty()) {
            println("ERROR: variable mapping does not contain all variables")
            return null
        }

        val newMatch = match.mapNotNull { s ->
            mapVariables(mapping, s)
        }.toSet()

        val newReplace = replace.mapNotNull { s ->
            mapVariables(mapping, s)
        }.toSet()

        return ActionMutationConfiguration(newMatch, newReplace, listOf())

    }

    private fun mapVariables(mapping : Map<RDFNode, RDFNode>, s : Statement) : Statement? {
        val newSubject =  mapping.getOrDefault(s.subject, s.subject)
        val newObject = mapping.getOrDefault(s.`object`, s.`object`)
        if (newSubject.isResource)
            return mf.createStatement(newSubject.asResource(), s.predicate, newObject)
        else {
            println("WARNING: $newSubject is not a resource. Can not create instantiated triple.")
            return null
        }
    }

}