package mutant.reasoning

import org.apache.jena.rdf.model.InfModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry

class CustomJenaApiReasoner(val model: Model, verbose : Boolean) : CustomReasoner(model, verbose) {
    private val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()
    private val inf : InfModel = ModelFactory.createInfModel(reasoner, model)

    override fun isConsistent(): Boolean {
        var consistent = true
        try {
            val validityReport = inf.validate()
            if (!validityReport.isValid) {
                for (reason in validityReport.reports) {
                    // ignore errors from range check, they do not work correctly for data properties with explicit range
                    // TODO: dive deeper into this problem and figure out how to solve it
                    if (reason.type.toString() != "\"range check\"") {
                        consistent = false
                    }
                }
            }
        } catch (e : Exception) {
            if (verbose)
                println("Exception in validation: $e --> consider as inconsistent")
            // validation failed --> play safe and assume ontology is inconsistent
            consistent = false
        }
        return consistent
    }

    override fun entailsAll(jenaModel: Model): Boolean {
        return inf.containsAll(jenaModel)
    }

    override fun containsAll(jenaModel: Model): Boolean {
        return model.containsAll(jenaModel.listStatements())
    }


}