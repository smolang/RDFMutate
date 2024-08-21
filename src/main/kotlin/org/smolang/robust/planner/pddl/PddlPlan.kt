package org.smolang.robust.planner.pddl

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.planner.ActionMutation
import org.smolang.robust.planner.ActionMutationConfiguration
import org.smolang.robust.planner.KgPddlMap

class PddlPlan(
    val actions : List<String>,
    val verbose : Boolean
) {
    override fun toString(): String {
        return actions.toString()
    }

    fun toMutationSequence(
        actionsToConfigs : MutableMap<String, ActionMutationConfiguration>,
        mapToPddl : KgPddlMap) : MutationSequence {

        val ms = MutationSequence(verbose)

        for (line in actions) {
            val elements = line.removePrefix("(").removeSuffix(")").split(" ")
            val action = elements[0]
            val variables = elements.subList(1, elements.size)

            // get config from action
            val config = actionsToConfigs[action]
            if (config == null)
                println("WARNING: no configuration found for action $action")
            else {
                val mapping: MutableMap<RDFNode, RDFNode> = mutableMapOf()
                // map variables to elements
                // will be used to replace variables in config with actual values, i.e. resources
                for (i in variables.indices) {
                    val rOld = config.variables[i]
                    val rNew = mapToPddl.toKg(variables[i])
                    if (rNew != null) {
                        mapping[rOld] = rNew
                    }
                }
                config.mapVariables(mapping)?.let { ms.addWithConfig(ActionMutation::class, it) }

            }
        }

        return  ms
    }
}