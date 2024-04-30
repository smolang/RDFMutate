package org.smolang.robust.mutant.reasoning

import openllet.owlapi.OpenlletReasoner
import openllet.owlapi.OpenlletReasonerFactory
import org.apache.jena.rdf.model.Model

class CustomOpenlletReasoner(jenaModel: Model,
                             verbose : Boolean) : OwlApiReasoner(jenaModel, verbose) {
    override fun initReasoner(): OpenlletReasoner? {
        return ontology?.let { OpenlletReasonerFactory.getInstance().createReasoner(it) }
    }
}