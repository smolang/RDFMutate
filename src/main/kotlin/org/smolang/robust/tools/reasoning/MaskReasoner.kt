package org.smolang.robust.tools.reasoning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.jena.rdf.model.Model

abstract class MaskReasoner(private val jenaModel : Model) {

    abstract fun isConsistent() : Boolean

}

class MaskReasonerFactory(private val reasoningBackend: ReasoningBackend) {
    fun getReasoner(model: Model) : MaskReasoner =
        when(reasoningBackend) {
            ReasoningBackend.OPENLLET -> MaskReasonerOpenllet(model)
            ReasoningBackend.HERMIT -> MaskReasonerHermit(model)
            ReasoningBackend.JENA -> MaskReasonerJenaApi(model)
            ReasoningBackend.NONE -> EmptyReasoner()
        }
}

@Serializable
enum class ReasoningBackend {
    @SerialName("hermit")
    HERMIT,
    @SerialName("pellet")
    OPENLLET,
    @SerialName("jena")
    JENA,
    @SerialName("none")
    NONE
}

