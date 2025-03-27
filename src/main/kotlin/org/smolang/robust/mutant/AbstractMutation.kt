package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// stores the information for one mutation but the information about the model is lacking
class AbstractMutation(private val mutOp: KClass<out Mutation>) {

    private var hasConfig : Boolean = false
    private var config : MutationConfiguration? = null

    private val mutatableAxioms: MutableSet<Statement> = hashSetOf()
    fun addMutatableAxiom(s: Statement) {
        mutatableAxioms.add(s)
    }

    constructor(mutation: KClass<out Mutation>,
                _config: MutationConfiguration) : this(mutation) {
        hasConfig = true
        config = _config
    }

    fun concreteMutation(model: Model) : Mutation {
        if (hasConfig) {
            val m = mutOp.primaryConstructor?.call(model) ?: Mutation(model)
            for (a in mutatableAxioms)
                m.addMutatableAxiom(a)
            config?.let { m.setConfiguration(it) }
            return m
        }
        else {
            val m= mutOp.primaryConstructor?.call(model) ?: Mutation(model)
            for (a in mutatableAxioms)
                m.addMutatableAxiom(a)
            return m
        }

    }

    override fun toString(): String {
        if (hasConfig)
            return "AbstractMutation representing mutation " +
                    "${mutOp.toString().removePrefix("class org.smolang.robust.mutant.")} " +
                    "with configuration $config"
        else
            return "AbstractMutation representing mutation " +
                    "${mutOp.toString().removePrefix("class org.smolang.robust.mutant.")} " +
                    "without configuration"
    }

}