package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.InfModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.smolang.robust.mainLogger

class MaskReasonerJenaApi(val model: Model) : MaskReasoner() {
    private val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()
    private val inf : InfModel = ModelFactory.createInfModel(reasoner, model)

    override fun isConsistent(): ConsistencyResult {
        try {
            val validityReport = inf.validate()
            if (!validityReport.isValid) {
                for (reason in validityReport.reports) {
                    // ignore errors from range check, they do not work correctly for data properties with explicit range
                    // TODO: dive deeper into this problem and figure out how to solve it
                    if (reason.type.toString() != "\"range check\"") {
                        ConsistencyResult.INCONSISTENT
                    }
                }
            }
        } catch (e : Exception) {
            mainLogger.error("Exception in Jana-API consistency check: $e")
            return ConsistencyResult.UNDECIDED
        }
        // no reasons found why result should be inconsistent
        return ConsistencyResult.CONSISTENT
    }

}