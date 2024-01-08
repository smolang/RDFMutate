package mutant.reasoning

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

abstract class OwlApiReasoner(jenaModel : Model,
                              verbose: Boolean) : CustomReasoner(jenaModel, verbose) {

    private val manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
    val ontology = owlApiOnt(jenaModel)
    val reasoner : OWLReasoner by lazy { initReasoner()}

    abstract fun initReasoner() : OWLReasoner

    override fun isConsistent(): Boolean {
        return reasoner.isConsistent
    }

    override fun entailsAll(jenaModel: Model): Boolean {
        // inconsistent ontology entails everything
        if (!isConsistent()) {
            return true
        }

        val entailedOntology = owlApiOnt(jenaModel)
        return reasoner.isEntailed(entailedOntology.axioms)
    }

    // converts Jena model into an OWL-API ontology
    private fun owlApiOnt(jenaModel: Model) : OWLOntology {

        // TODO: get this direct conversion running, seems to be some problem with Maven dependencies...
        // use ONT-API to pass Jena model to OWL API
        //var manager: OntologyManager = OntManagers.createManager()
        //var ontology: Ontology = manager.addOntology(jenaModel.graph)

        // temporary solution: write ontology to stream using Jena API
        // and read from stream with OWL API
        // problem: probably rather slow...
        val ontology: OWLOntology

        ByteArrayOutputStream().use { outStream ->
            RDFDataMgr.write(outStream, jenaModel, Lang.RDFXML)
            ByteArrayInputStream(outStream.toByteArray()).use { inStream ->
                ontology = manager.loadOntologyFromOntologyDocument(inStream)
            }
        }
        return ontology
    }

}