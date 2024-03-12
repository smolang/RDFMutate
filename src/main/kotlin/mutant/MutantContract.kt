package mutant

import mutant.reasoning.CustomReasonerFactory
import mutant.reasoning.ReasoningBackend
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory


class MutantContract(val verbose: Boolean) {
    var entailedModel : Model = ModelFactory.createDefaultModel()
    var containedModel : Model = ModelFactory.createDefaultModel()

    // additional axioms that are added to the ontology before reasoning, e.g. containing axioms not considered while
    // creating the mutations
    var additionalAxioms: Model = ModelFactory.createDefaultModel()

    // default: use Openllet for reasoning
    var reasoningBackend : ReasoningBackend = ReasoningBackend.OPENLLET



    // checks, if the provided model is valid w.r.t. the contract
    fun validate(model: Model) : Boolean {

        for (a in additionalAxioms.listStatements()) {
            model.add(a)
        }

        // create reasoner with the selected backend
        val reasonerFactory = CustomReasonerFactory(verbose)
        val reasoner = reasonerFactory.getReasoner(model, reasoningBackend)

        val consistent = reasoner.isConsistent()
        // alsways use JENA-API for containment check
        val containment = model.containsAll(containedModel)
        val entailment = reasoner.entailsAll(entailedModel)

        return  consistent
                && containment
                && entailment
    }
}