package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry

class MutantContract(val verbose: Boolean) {
    var entailedModel : Model = ModelFactory.createDefaultModel()
    var containedModel : Model = ModelFactory.createDefaultModel()


    // checks, if the provided model is valid w.r.t. the contract
    fun validate(model: Model) : Boolean {
        val reasoner = ReasonerRegistry.getOWLReasoner()
        val inf = ModelFactory.createInfModel(reasoner, model)

        var consistent = true
        try {
            val validityReport = inf.validate()
            if (!validityReport.isValid) {
                for (reason in validityReport.reports) {
                    // ignore errors from range check, they do not work correctly for data properties with explicit range
                    // TODO: dive deeper into this problem and figure out how to solve it
                    if (reason.type.toString() != "\"range check\"")
                        consistent = false
                }
            }
        } catch (e : Exception) {
            if (verbose)
                println("Exception in validation: $e --> consider as inconsistent")
            // validation failed --> play safe and assume ontology is inconsistent
            consistent = false
        }

        if (!consistent && verbose)
            println("mutation is inconsistent")

        return  consistent
                && model.containsAll(containedModel)
                && inf.containsAll(entailedModel)
    }
}