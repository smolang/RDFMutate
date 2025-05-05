package org.smolang.robust.tools

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import org.apache.jena.rdf.model.Model
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.smolang.robust.mainLogger
import java.io.File

class OwlOntologyInterface {
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
            handleSaveError("Could not translate KG to OWL ontology.", outputFile)
            return
        }

        // save to file (functional syntax)
        try {
            manager.saveOntology(
                ontology,
                FunctionalSyntaxDocumentFormat(),
                IRI.create(outputFile.toURI())
            )
        } catch (e: Exception) {
            handleSaveError("raised exception: $e", outputFile)
        }


    }

    // function to handle error when saving document
    private fun handleSaveError(problem: String, outputFile: File) {
        val manager = OntManagers.createManager()
        mainLogger.error("Could not save ontology. Trying to save empty ontology. Problem: $problem")
        try {
            manager.saveOntology(
                manager.createOntology(),
                FunctionalSyntaxDocumentFormat(),
                IRI.create(outputFile.toURI())
            )
        } catch (e: Exception) {
            mainLogger.error("Could not save ontology in file $outputFile.")
        }
    }

    // converts jena model into OWL ontology.
    fun jenaToOwl(model: Model) : OWLOntology? {

        val manager = OntManagers.createManager()
        manager.ontologyConfigurator.isIgnoreAxiomsReadErrors = true;

        // add all OWLAxioms from Jena Model:
        // obtains from https://github.com/owlcs/ont-api/wiki/Examples#-2-ont-api-rdf-model-interface
        try {
            return manager.addOntology(model.getGraph());
        }
        catch (e: Exception) {
            // try stream-based method: more reliable but less elegant
            mainLogger.error("Could not extract OWL axioms from Jena Model. Raised exception: $e")
        }
        return null
    }
}