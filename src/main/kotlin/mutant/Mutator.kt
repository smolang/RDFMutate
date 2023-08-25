package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


class Mutator(private val mutSeq: MutationSequence, private val verbose: Boolean) {
    fun mutate (seed : Model) : Model {
        var target = seed
        for (i  in 0 until mutSeq.size()) {
            val mutation = mutSeq[i].concreteMutation(target)
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
