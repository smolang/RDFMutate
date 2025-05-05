package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.Model
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.reasoner.OWLReasoner

class MaskReasonerElk(jenaModel: Model) : OwlApiReasoner(jenaModel) {
    override fun initReasoner(): OWLReasoner? {
        return ontology?.let { ElkReasonerFactory().createReasoner(it) }
    }

}