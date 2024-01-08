package mutant.reasoning

import org.apache.jena.rdf.model.Model

enum class ReasoningBackend {
    HERMIT, OPENLLET, JENA
}
class CustomReasonerFactory(private val verbose: Boolean) {

    fun getReasoner(model: Model, reasoningBackend: ReasoningBackend) : CustomReasoner {
        val reasoner = when(reasoningBackend) {
            ReasoningBackend.OPENLLET -> CustomOpenlletReasoner(model, verbose)
            ReasoningBackend.HERMIT -> CustomHermitReasoner(model, verbose)
            ReasoningBackend.JENA -> CustomJenaApiReasoner(model, verbose)
        }

        return reasoner
    }
}