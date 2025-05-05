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
import org.smolang.robust.tools.OwlOntologyInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

abstract class OwlApiReasoner(jenaModel : Model) : MaskReasoner() {

    private val manager: OWLOntologyManager = OWLManager.createOWLOntologyManager()
    val ontology = owlApiOnt(jenaModel)
    private val reasoner : OWLReasoner? by lazy { initReasoner()}

    abstract fun initReasoner() : OWLReasoner?

    override fun isConsistent(): ConsistencyResult {
        return try {
            if (reasoner == null)
                return ConsistencyResult.UNDECIDED

            return boolToConsistencyResult(reasoner!!.isConsistent)
        }
        catch (e : Exception) {
            mainLogger.error("mask reasoner raised exception. Threat result as \"false\". ${e.toString()}")
            ConsistencyResult.UNDECIDED
        }
    }


    // converts Jena model into an OWL-API ontology
    private fun owlApiOnt(jenaModel: Model) : OWLOntology? {
        return OwlOntologyInterface().jenaToOwl(jenaModel)

    }

}