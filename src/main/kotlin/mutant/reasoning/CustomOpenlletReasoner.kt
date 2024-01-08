package mutant.reasoning

import openllet.owlapi.OpenlletReasonerFactory
import org.apache.jena.rdf.model.Model
import org.semanticweb.owlapi.reasoner.OWLReasoner

class CustomOpenlletReasoner(jenaModel: Model,
                             verbose : Boolean) : OwlApiReasoner(jenaModel, verbose) {
    override fun initReasoner(): OWLReasoner {
        return OpenlletReasonerFactory.getInstance().createReasoner(ontology)
    }
}