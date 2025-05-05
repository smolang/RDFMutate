package org.smolang.robust.tools.reasoning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.jena.rdf.model.Model

abstract class MaskReasoner() {

    abstract fun isConsistent() : ConsistencyResult

    fun boolToConsistencyResult(consistent: Boolean) : ConsistencyResult {
        return if (consistent)
            ConsistencyResult.CONSISTENT
        else
            ConsistencyResult.INCONSISTENT
    }
}

class MaskReasonerFactory(private val reasoningBackend: ReasoningBackend) {
    fun getReasoner(model: Model) : MaskReasoner =
        when(reasoningBackend) {
            ReasoningBackend.OPENLLET -> MaskReasonerOpenllet(model)
            ReasoningBackend.HERMIT -> MaskReasonerHermit(model)
            ReasoningBackend.JENA -> MaskReasonerJenaApi(model)
            ReasoningBackend.ELK -> MaskReasonerElk(model)
            ReasoningBackend.NONE -> EmptyReasoner()
        }
}

enum class ConsistencyResult {
    CONSISTENT,
    INCONSISTENT,
    UNDECIDED
}

@Serializable
enum class ReasoningBackend {
    @SerialName("hermit")
    HERMIT,
    @SerialName("pellet")
    OPENLLET,
    @SerialName("jena")
    JENA,
    @SerialName("elk")
    ELK,
    @SerialName("none")
    NONE
}

