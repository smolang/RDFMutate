package org.smolang.robust.tools.reasoning

import openllet.atom.OpenError
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.smolang.robust.mainLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

abstract class OwlApiReasoner(jenaModel : Model) : MaskReasoner(jenaModel) {

    private val manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
    val ontology = owlApiOnt(jenaModel)
    private val reasoner : OWLReasoner? by lazy { initReasoner()}

    abstract fun initReasoner() : OWLReasoner?

    override fun isConsistent(): Boolean {
        return try {
            reasoner?.isConsistent ?: false
        }
        catch (e : Exception) {
            // treat ontologies that result in exception of reasoner as inconsistent
            when ( e) {
                is OpenError, is ClassCastException -> {
                    //known exceptions
                    false
                }
                else -> {
                    mainLogger.error(e.toString())
                    throw e
                }
            }
        }
    }


    // converts Jena model into an OWL-API ontology
    private fun owlApiOnt(jenaModel: Model) : OWLOntology? {

        // TODO: get this direct conversion running, seems to be some problem with Maven dependencies...
        // use ONT-API to pass Jena model to OWL API
        //var manager: OntologyManager = OntManagers.createManager()
        //var ontology: Ontology = manager.addOntology(jenaModel.graph)

        // temporary solution: write ontology to stream using Jena API
        // and read from stream with OWL API
        // problem: probably rather slow...
        val ontology: OWLOntology?

        ByteArrayOutputStream().use { outStream ->
            RDFDataMgr.write(outStream, jenaModel, Lang.RDFXML)
            ByteArrayInputStream(outStream.toByteArray()).use { inStream ->
                ontology =
                    try {
                        manager.loadOntologyFromOntologyDocument(inStream)
                    }
                    catch (e : Exception){
                        mainLogger.warn("reasoner could not read ontology because of exception $e")
                        null
                    }
            }
        }
        return ontology
    }

}