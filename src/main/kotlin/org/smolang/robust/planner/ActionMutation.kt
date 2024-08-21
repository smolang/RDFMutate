package org.smolang.robust.planner

import org.apache.jena.rdf.model.Model
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.Mutation
import org.smolang.robust.mutant.MutationConfiguration
import kotlin.reflect.KClass

class ActionMutation(model: Model, verbose: Boolean) : Mutation(model, verbose) {

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is ActionMutationConfiguration)
        super.setConfiguration(_config)
    }

    override fun isApplicable(): Boolean {
        if (!hasConfig)
            return false

        if (config !is ActionMutationConfiguration)
            return false

        // is only applicable if there are no variables, i.e. everything are statements occurring in ontology
        if ((config as ActionMutationConfiguration).variables.isNotEmpty())
            return false

        return true
    }

    override fun createMutation() {
        assert(hasConfig && config is ActionMutationConfiguration)
        val actionConfig = config as ActionMutationConfiguration
        addSet = actionConfig.replace.toMutableSet()
        removeSet = actionConfig.match.toMutableSet()
        super.createMutation()
    }

}