package org.smolang.robust.domainSpecific.reasoner

import arrow.typeclasses.Hash
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement

// class to analyze an ontology
class OntologyAnalyzer {
    private val owlPrefix = "http://www.w3.org/2002/07/owl#"
    private val rdfsPrefix = "http://www.w3.org/2000/01/rdf-schema#"
    private val rdfPrefix = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val xsdPrefix = "http://www.w3.org/2001/XMLSchema"

    // returns set with all used owl features, i.e. IRIs that start with "owl" + some extra
    fun getOwlFeatures(model: Model): Set<Resource> {
        val usedFeatures = mutableSetOf<Resource>()

        for (s in model.listStatements()) {
            usedFeatures.addAll(getOWLFeatures(s))
        }

        return usedFeatures
    }

    // return hashed features --> safes space
    fun getOwlFeaturesHashed(model: Model): Set<Int> {
        val usedFeatures = mutableSetOf<Int>()

        for (s in model.listStatements()) {
            usedFeatures.addAll(getOWLFeatures(s).map { it.toString().hashCode() })
        }

        return usedFeatures
    }

    private fun getOWLFeatures(s: Statement) : Set<Resource> {
        val usedFeatures = mutableSetOf<Resource>()
        if (isOwlResource(s.subject))
            usedFeatures.add(s.subject)

        if (isOwlResource(s.predicate))
            usedFeatures.add(s.predicate)

        if (s.`object`.isResource && isOwlResource(s.`object`.asResource()))
            usedFeatures.add(s.`object`.asResource())

        return usedFeatures
    }



    private fun isOwlResource(r: Resource) : Boolean {
        if (r.uri== null)
            return false

        return r.uri.startsWith(owlPrefix) ||
                r.uri.startsWith(rdfsPrefix) ||
                r.uri.startsWith(rdfPrefix) ||
                r.uri.startsWith(xsdPrefix)
    }
}