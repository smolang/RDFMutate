package org.smolang.robust.domainSpecific.reasoner

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.domainSpecific.KgAnalyzer

// class to analyze an ontology
class OwlOntologyAnalyzer : KgAnalyzer() {
    private val owlPrefix = "http://www.w3.org/2002/07/owl#"
    private val rdfsPrefix = "http://www.w3.org/2000/01/rdf-schema#"
    private val rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val xsdPrefix = "http://www.w3.org/2001/XMLSchema"



    override fun isFeature(r: Resource) : Boolean {
        if (r.uri== null)
            return false

        return r.uri.startsWith(owlPrefix) ||
                r.uri.startsWith(rdfsPrefix) ||
                r.uri.startsWith(rdfPrefix) ||
                r.uri.startsWith(xsdPrefix)
    }
}