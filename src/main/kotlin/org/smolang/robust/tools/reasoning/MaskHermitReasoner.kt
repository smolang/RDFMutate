package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.Model
import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.owlapi.reasoner.OWLReasoner

class MaskHermitReasoner(jenaModel: Model) : OwlApiReasoner(jenaModel) {
    override fun initReasoner(): OWLReasoner? {
        return ontology?.let { ReasonerFactory().createReasoner(it) }
    }
}