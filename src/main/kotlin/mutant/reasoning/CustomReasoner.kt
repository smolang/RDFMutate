package mutant.reasoning

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement




abstract class CustomReasoner(private val jenaModel : Model,
                              val verbose : Boolean) {

    abstract fun isConsistent() : Boolean

    abstract fun entailsAll(jenaModel: Model) : Boolean

    abstract fun containsAll(jenaModel: Model): Boolean

    fun entails(s : Statement) : Boolean {
        // treat statement as a model and check that for entailment
        val m : Model = ModelFactory.createDefaultModel()
        m.add(s)
        return entailsAll(m)
    }
}