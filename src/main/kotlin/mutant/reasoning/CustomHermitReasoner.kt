package mutant.reasoning

import org.apache.jena.rdf.model.Model
import org.semanticweb.HermiT.ReasonerFactory as HermiTReasonerFactory
import org.semanticweb.owlapi.reasoner.OWLReasoner

class CustomHermitReasoner(jenaModel: Model,
                           verbose :Boolean) : OwlApiReasoner(jenaModel, verbose) {
    override fun initReasoner(): OWLReasoner? {
        return ontology?.let { HermiTReasonerFactory().createReasoner(it) }
    }
}