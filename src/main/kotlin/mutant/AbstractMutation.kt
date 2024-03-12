package mutant

import org.apache.jena.rdf.model.Model
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// stores the information for one mutation but the information about the model is lacking
class AbstractMutation(private val mutOp: KClass<out Mutation>,
                       private val verbose: Boolean ) {

    var hasConfig : Boolean = false
    var config : MutationConfiguration? = null

    val relevantPrefixes: MutableSet<String> = hashSetOf()
    fun addRelevantPrefix(prefix: String) {
        relevantPrefixes.add(prefix)
    }

    constructor(mutation: KClass<out Mutation>,
                _config: MutationConfiguration,
                verbose : Boolean) : this(mutation, verbose) {
        hasConfig = true
        config = _config
    }

    fun concreteMutation(model: Model) : Mutation {
        if (hasConfig) {
            val m = mutOp.primaryConstructor?.call(model, verbose) ?: Mutation(model, verbose)
            for (p in relevantPrefixes)
                m.addRelevantPrefix(p)
            config?.let { m.setConfiguration(it) }
            return m
        }
        else {
            val m= mutOp.primaryConstructor?.call(model, verbose) ?: Mutation(model, verbose)
            for (p in relevantPrefixes)
                m.addRelevantPrefix(p)
            return m
        }

    }

}