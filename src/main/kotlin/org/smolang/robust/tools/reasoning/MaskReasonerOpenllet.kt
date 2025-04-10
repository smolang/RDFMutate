package org.smolang.robust.tools.reasoning

import openllet.owlapi.OpenlletReasoner
import openllet.owlapi.OpenlletReasonerFactory
import org.apache.jena.rdf.model.Model

class MaskReasonerOpenllet(jenaModel: Model) : OwlApiReasoner(jenaModel) {
    override fun initReasoner(): OpenlletReasoner? {
        return ontology?.let { OpenlletReasonerFactory.getInstance().createReasoner(it) }
    }
}