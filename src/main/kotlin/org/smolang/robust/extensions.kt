package org.smolang.robust

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement

// provides a string representation of a statement using local names (if possible)
fun Statement.toLocalString() : String {
    val localObj = if (this.`object`.isResource) this.`object`.asResource().localName else this.`object`.toString()
    return "[${this.subject.localName}, ${this.predicate.localName}, $localObj]"
}

// check, if statement contains item
fun Statement.containsResource(item : Resource) : Boolean {
    return subject == item || predicate == item ||
            (`object`.isResource && `object`.asResource() == item)
}

