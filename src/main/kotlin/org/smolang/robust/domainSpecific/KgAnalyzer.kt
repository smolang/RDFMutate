package org.smolang.robust.domainSpecific

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement

abstract class KgAnalyzer {
    // returns set with all used features, i.e. IRIs that are specified using "isFeature" function
    fun getFeatures(model: Model): Set<Resource> {
        val usedFeatures = mutableSetOf<Resource>()

        for (s in model.listStatements()) {
            usedFeatures.addAll(getFeatures(s))
        }

        return usedFeatures
    }

    // return hashed features --> safes space
    fun getFeaturesHashed(model: Model): Set<Int> {
        val usedFeatures = mutableSetOf<Int>()

        for (s in model.listStatements()) {
            usedFeatures.addAll(getFeatures(s).map { it.toString().hashCode() })
        }

        return usedFeatures
    }

    fun getFeatures(statements : List<Statement>): Set<Resource> {
        val usedFeatures = mutableSetOf<Resource>()

        for (s in statements) {
            usedFeatures.addAll(getFeatures(s))
        }

        return usedFeatures
    }

    private fun getFeatures(s: Statement) : Set<Resource> {
        val usedFeatures = mutableSetOf<Resource>()
        if (isFeature(s.subject))
            usedFeatures.add(s.subject)

        if (isFeature(s.predicate))
            usedFeatures.add(s.predicate)

        if (s.`object`.isResource && isFeature(s.`object`.asResource()))
            usedFeatures.add(s.`object`.asResource())

        return usedFeatures
    }

    abstract fun isFeature(r: Resource) : Boolean
}