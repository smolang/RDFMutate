package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement

class ComplexStatementBuilder(val model: Model) {

    val rdfTypeProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val subClassProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")
    val equivClassProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#equivalentClass")
    val disjointClassProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#disjointWith")

    val subPropertyProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
    val equivPropertyProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#equivalentProperty")
    val domainProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#domain")
    val rangeProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#range")
    val funcProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#FunctionalProperty")
    val reflexiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#ReflexiveProperty")
    val irreflexiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#IrreflexiveProperty")
    val transitiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#TransitiveProperty")
    val oneOfProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#oneOf")

    val owlThing : Resource = model.createResource("http://www.w3.org/2002/07/owl#Thing")
    val owlNothing : Resource = model.createResource("http://www.w3.org/2002/07/owl#Nothing")

    val owlClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Class")
    val namedInd : Resource = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectPropClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
    val dataPropClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#DatatypeProperty")
    val restrictionClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Restriction")

    val negPropAssertion : Resource = model.createResource("http://www.w3.org/2002/07/owl#NegativePropertyAssertion")
    val sourceIndProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#sourceIndividual")
    val assertionPropProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#assertionProperty")
    val targetIndProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#targetIndividual")
    val targetValue : Property = model.createProperty("http://www.w3.org/2002/07/owl#targetValue")


    val differentFromProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#differentFrom")
    val sameAsProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#sameAs")
    val propChainProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#propertyChainAxiom")

    val xsdBoolean : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#boolean")
    val xsdDecimal : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#decimal")
    val xsdDouble : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#double")
    val rdfsLiteral : Resource = model.createResource("http://www.w3.org/2000/01/rdf-schema#Literal")

    val datatypeClass : Resource = model.createResource("http://www.w3.org/2000/01/rdf-schema#Datatype")

    val intersectionProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#intersectionOf")
    val unionProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#unionOf")
    val someValuesFromProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#someValuesFrom")
    val hasValueProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#hasValue")
    val hasSelfProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#hasSelf")
    val onPropertyProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#onProperty")

    val rdfListClass : Resource = model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
    val rdfFirst : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
    val rdfRest : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
    val rdfNil : Resource = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil")

    private val languageTags = listOf("en", "zh", "hi", "es",  "fr", "de")


    fun propertyChain(links: List<Resource>, superProp : Resource) : List<Statement> {

        val result = mutableListOf<Statement>()
        val axiomNode = model.createResource()


        // from: https://www.w3.org/TR/2012/REC-owl2-mapping-to-rdf-20121211/
        result.add(model.createStatement(superProp, propChainProp, axiomNode))
        result.addAll(sequenceOf(axiomNode, links))

        return result
    }

    // creates triples for "dataOneOf" construct, referring to set containing only "data"
    fun dataOneOf(head: Resource, data: Literal) : List<Statement> {
        val result = mutableListOf<Statement>()

        val listHead = model.createResource()
        result.add(model.createStatement(head, rdfTypeProp, datatypeClass))
        result.add(model.createStatement(head, oneOfProp, listHead))
        result.addAll(sequenceOf(listHead, listOf(data)))

        return result
    }

    // creates triples for "objectIntersectionOf" construct
    fun objectIntersectionOf(head: Resource, classes : List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, owlClass))

        val listHead = model.createResource()
        result.add(model.createStatement(head, intersectionProp, listHead))
        result.addAll(sequenceOf(listHead, classes))

        return result
    }

    // creates triples for "objectOneOf" construct
    fun objectOneOf(head: Resource, elements: List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, owlClass))

        val listHead = model.createResource()
        result.add(model.createStatement(head, oneOfProp, listHead))
        result.addAll(sequenceOf(listHead, elements))

        return result
    }

    // creates triples for "objectsSomeValuesFrom" construct
    fun someValuesFrom(head: Resource, targetProperty: Resource, targetClass: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, restrictionClass))
        result.add(model.createStatement(head, onPropertyProp, targetProperty))
        result.add(model.createStatement(head, someValuesFromProp, targetClass))

        return result
    }

    // creates triples for "objectHasValue" construct
    fun objectHasValue(head: Resource, targetProperty: Resource, individual: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, restrictionClass))
        result.add(model.createStatement(head, onPropertyProp, targetProperty))
        result.add(model.createStatement(head, hasValueProp, individual))

        return result
    }

    // creates triples for "objectHasSelf" construct
    fun objectHasSelf(head: Resource, targetProperty: Resource) : List<Statement> {
        val result = mutableListOf<Statement>()
        val trueLiteral = model.createTypedLiteral("true", xsdBoolean.toString())

        result.add(model.createStatement(head, rdfTypeProp, restrictionClass))
        result.add(model.createStatement(head, onPropertyProp, targetProperty))
        result.add(model.createStatement(head, hasSelfProp, trueLiteral))

        return result
    }

    // creates triples for "dataIntersectionOf" construct
    fun dataIntersectionOf(head: Resource, dataRanges : List<Resource>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, datatypeClass))

        val listHead = model.createResource()
        result.add(model.createStatement(head, intersectionProp, listHead))
        result.addAll(sequenceOf(listHead, dataRanges))

        return result
    }

    // creates triples for "dataOneOf" construct
    fun dataOneOf(head: Resource, literals : List<RDFNode>) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, datatypeClass))

        val listHead = model.createResource()
        result.add(model.createStatement(head, oneOfProp, listHead))
        result.addAll(sequenceOf(listHead, literals))

        return result
    }

    // creates triples for "dataHasValue" construct
    fun dataHasValue(head: Resource, targetProperty: Resource, literal: RDFNode) : List<Statement> {
        val result = mutableListOf<Statement>()

        result.add(model.createStatement(head, rdfTypeProp, restrictionClass))
        result.add(model.createStatement(head, onPropertyProp, targetProperty))
        result.add(model.createStatement(head, hasValueProp, literal))

        return result
    }


    fun sequenceOf(head: Resource, data : List<RDFNode> ) : List<Statement> {
        val result = mutableListOf<Statement>()
        var current = head
        for (d in data) {
            result.add(model.createStatement(current, rdfFirst, d))
            if (d != data.last()) {
                val newNode = model.createResource()
                result.add(model.createStatement(current, rdfRest, newNode))
                current = newNode
            }
            else
                result.add(model.createStatement(current, rdfRest, rdfNil))
        }
        return result
    }


}