package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF

class ComplexTermParser {
    // returns all elements from the linked a list in the ontology that starts in "head"
    fun allElementsInList(model: Model, head: Resource) : List<RDFNode> {

        val list : MutableList<RDFNode> = mutableListOf()
        // add all (i.e. one) current element
        for (element in model.listObjectsOfProperty(head, RDF.first))
            list.add(element)

        // check if there is more list to come

        val rest = model.listObjectsOfProperty(head, RDF.rest).toSet()
        if (rest.contains(RDF.nil) || rest.isEmpty())
        // base case + if rest is empty --> error in schema as end is not correctly marked
            return list
        else {
            // recursive call
            val restElements = allElementsInList(model, rest.single().asResource())
            list.addAll(restElements)
            return list
        }

    }
}