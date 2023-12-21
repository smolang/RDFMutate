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
        val validityReport = inf.validate()
        println("consistency: " + validityReport.isValid)
        if (validityReport.isValid()) {
            println("OK")
        } else {
            println("Conflicts")
            val i: Iterator<*> = validityReport.getReports()
            while (i.hasNext()) {
                println(" - " + i.next())
            }
        }


        return  model.containsAll(containedModel)
                && inf.containsAll(entailedModel)
    }
}