package org.smolang.robust.domainSpecific.reasoner

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.internal.AxiomTranslator
import org.apache.jena.ontapi.OntModelFactory
import org.apache.jena.rdf.model.Model
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat
import org.semanticweb.owlapi.model.AxiomType
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAxiom
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
        val axioms: MutableSet<OWLAxiom> = mutableSetOf()

        // add all OWLAxioms from Jena Model:
        // obtains from https://github.com/owlcs/ont-api/wiki/Examples#-2-ont-api-rdf-model-interface
        AxiomType.AXIOM_TYPES.stream()
            .map { type -> AxiomTranslator.get(type) }
            .forEach { t ->
                t.axioms(OntModelFactory.createModel(model.graph))
                    .forEach { axiom -> axioms.add(axiom.owlObject)}
            }

        // collect axioms and save to file (functional syntax)
        val ontology = manager.createOntology(axioms)
        manager.saveOntology(ontology, FunctionalSyntaxDocumentFormat(),  IRI.create(outputFile.toURI()))

    }
}