package org.smolang.robust.domainSpecific.reasoner

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyFactory
import com.github.owlcs.ontapi.internal.AxiomTranslator
import org.apache.jena.ontapi.OntModelFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat
import org.semanticweb.owlapi.model.AxiomType
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAxiom
import org.semanticweb.owlapi.model.OWLDataFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.smolang.robust.mainLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class OwlFileHandler {
    // uses ont-api to load ontologies, e.g. in functional syntax and serialize them as RDF
    fun loadOwlDocument(file: File) : Model {
        val manager = OntManagers.createManager()
        val ontology: Ontology = manager.loadOntologyFromOntologyDocument(file)
        return ontology.asGraphModel()
    }

    // uses ont-api to save only those parts of the model that represent valid DL axioms
    fun saveOwlDocument(model: Model, outputFile: File) {
        val manager = OntManagers.createManager()

        val ontology = jenaToOwl(model)


        if (ontology == null) { // can not save ontology
            mainLogger.error("Could not translate triples to OWL ontology. Save empty ontology instead.")
            manager.saveOntology(
                manager.createOntology(),
                FunctionalSyntaxDocumentFormat(),
                IRI.create(outputFile.toURI())
            )
            return
        }

        // save to file (functional syntax)
        manager.saveOntology(
            ontology,
            FunctionalSyntaxDocumentFormat(),
            IRI.create(outputFile.toURI())
        )

    }

    // converts jena model into OWL ontology.
    private fun jenaToOwl(model: Model) : OWLOntology? {

        val manager = OntManagers.createManager()
        val axioms: MutableSet<OWLAxiom> = mutableSetOf()

        // add all OWLAxioms from Jena Model:
        // obtains from https://github.com/owlcs/ont-api/wiki/Examples#-2-ont-api-rdf-model-interface
        try {
            AxiomType.AXIOM_TYPES.stream()
                .map { type -> AxiomTranslator.get(type) }
                .forEach { t ->
                    t.axioms(OntModelFactory.createModel(model.graph))
                        .forEach { axiom ->
                            axioms.add(axiom.owlObject)
                        }
                }

            // collect axioms
            return manager.createOntology(axioms)
        }
        catch (e: Exception) {
            // try stream-based method: more reliable but less elegant
            mainLogger.error("Could not extract OWL axioms from Jena Model. Raised exception: $e")
        }
        return null
    }
}