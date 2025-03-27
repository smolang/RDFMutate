package org.smolang.robust.tools

import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD

class ComplexStatementBuilder(val model: Model) {

    private val languageTags = listOf("en", "zh", "hi", "es",  "fr", "de")


    fun propertyChain(links: List<Resource>, superProp : Resource) : List<Statement> {

        val result = mutableListOf<Statement>()
        val axiomNode = model.createResource()


        // from: https://www.w3.org/TR/2012/REC-owl2-mapping-to-rdf-20121211/
        result.add(model.createStatement(superProp, OWL.propertyChainAxiom, axiomNode))
        result.addAll(sequenceOf(axiomNode, links))

        return result
    }

    // creates triples for "dataOneOf" construct, referring to set containing only "data"
    fun dataOneOf(head: Resource, data: Literal) : List<Statement> {
        val result = mutableListOf<Statement>()

        val listHead = model.createResource()
        result.add(model.createStatement(head, RDF.type, RDFS.Datatype))
        result.add(model.createStatement(head, OWL.oneOf, listHead))
        result.addAll(sequenceOf(listHead, listOf(data)))

        return result
    }

    // creates triples for "objectIntersectionOf" construct
    fun objectIntersectionOf(head: Resource, classes : List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, OWL.Class))

        val listHead = model.createResource()
        result.add(model.createStatement(head, OWL.intersectionOf, listHead))
        result.addAll(sequenceOf(listHead, classes))

        return result
    }

    // creates triples for "objectOneOf" construct
    fun objectOneOf(head: Resource, elements: List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, OWL.Class))

        val listHead = model.createResource()
        result.add(model.createStatement(head, OWL.oneOf, listHead))
        result.addAll(sequenceOf(listHead, elements))

        return result
    }

    // creates triples for "objectsSomeValuesFrom" construct
    fun someValuesFrom(head: Resource, targetProperty: Resource, targetClass: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, OWL.Restriction))
        result.add(model.createStatement(head, OWL.onProperty, targetProperty))
        result.add(model.createStatement(head, OWL.someValuesFrom, targetClass))

        return result
    }

    // creates triples for "objectHasValue" construct
    fun objectHasValue(head: Resource, targetProperty: Resource, individual: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, OWL.Restriction))
        result.add(model.createStatement(head, OWL.onProperty, targetProperty))
        result.add(model.createStatement(head, OWL.hasValue, individual))

        return result
    }

    // creates triples for "objectHasSelf" construct
    fun objectHasSelf(head: Resource, targetProperty: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()
        val trueLiteral = model.createTypedLiteral("true", XSD.xboolean.toString())

        result.add(model.createStatement(head, RDF.type, OWL.Restriction))
        result.add(model.createStatement(head, OWL.onProperty, targetProperty))
        result.add(model.createStatement(head, OWL.hasSelf, trueLiteral))

        return result
    }

    // creates triples for "dataIntersectionOf" construct
    fun dataIntersectionOf(head: Resource, dataRanges : List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, RDFS.Datatype))

        val listHead = model.createResource()
        result.add(model.createStatement(head, OWL.intersectionOf, listHead))
        result.addAll(sequenceOf(listHead, dataRanges))

        return result
    }

    // creates triples for "dataOneOf" construct
    fun dataOneOf(head: Resource, literals : List<RDFNode>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, RDFS.Datatype))

        val listHead = model.createResource()
        result.add(model.createStatement(head, OWL.oneOf, listHead))
        result.addAll(sequenceOf(listHead, literals))

        return result
    }

    // creates triples for "dataHasValue" construct
    fun dataHasValue(head: Resource, targetProperty: Resource, literal: RDFNode) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, RDF.type, OWL.Restriction))
        result.add(model.createStatement(head, OWL.onProperty, targetProperty))
        result.add(model.createStatement(head, OWL.hasValue, literal))

        return result
    }


    fun sequenceOf(head: Resource, data : List<RDFNode> ) : List<Statement> {
        val result = mutableListOf<Statement>()
        var current = head
        for (d in data) {
            result.add(model.createStatement(current, RDF.first, d))
            if (d != data.last()) {
                val newNode = model.createResource()
                result.add(model.createStatement(current, RDF.rest, newNode))
                current = newNode
            }
            else
                result.add(model.createStatement(current, RDF.rest, RDF.nil))
        }
        return result
    }


}