package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class Mutator(private val mutOps: List<KClass<out Mutation>>, private val verbose: Boolean) {
    fun mutate(seed : Model, rounds : Int) : Model{
        var i = 0
        var target = seed
        while(i++ < rounds) {
            val mutations = mutOps.map { it.primaryConstructor!!.call(target, verbose) }
            val mutation = mutations.random()
            if(mutation.isApplicable())  target = mutation.applyCopy()
        }

        return target
    }

    fun validate(model: Model, contract : Model) : Boolean{
        val reasoner = ReasonerRegistry.getOWLReasoner()
        val inf = ModelFactory.createInfModel(reasoner, model)
        return inf.containsAll(contract)
    }
}