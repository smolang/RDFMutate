package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.RDFDataMgr
import java.io.File

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

// import jena model from file, depending on ending
fun File.getJenaModel(): Model {
    return if (this.name.endsWith(".owl"))
        OwlOntologyInterface().loadOwlDocument(this)
    else
        RDFDataMgr.loadDataset(this.absolutePath).defaultModel
}

