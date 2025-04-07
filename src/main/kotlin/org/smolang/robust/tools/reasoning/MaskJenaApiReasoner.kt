package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.InfModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.smolang.robust.mainLogger

class MaskJenaApiReasoner(val model: Model) : MaskReasoner(model) {
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
            mainLogger.warn("Exception in validation: $e --> consider as inconsistent")
            // validation failed --> play safe and assume ontology is inconsistent
            consistent = false
        }
        return consistent
    }

}