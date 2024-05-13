package org.smolang.robust.mutant

import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


// turns OWL triples into SHACL shapes
class ShapeGenerator() {

    val shapes : Model = ModelFactory.createDefaultModel()

    val prefixSchema : String= "http://schema.org/"
    val prefixShacl : String = "http://www.w3.org/ns/shacl#"

    val nodeShape : Resource = shapes.getResource(prefixShacl + "NodeShape")
    val targetNode : Property = shapes.getProperty(prefixShacl + "targetNode")
    val shPropertyProperty : Property = shapes.getProperty(prefixShacl + "property")
    val shPathProperty : Property = shapes.getProperty(prefixShacl + "path")
    val shInProperty : Property = shapes.getProperty(prefixShacl + "in")
    val shHasValueProperty : Property = shapes.getProperty(prefixShacl + "hasValue")

    val shMinCountProperty : Property = shapes.getProperty(prefixShacl + "minCount")

    val typeProp : Property = shapes.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

    fun turnAxiomsToShapes(maskFile : String) {
        // register prefixes
        shapes.setNsPrefix("sh",prefixShacl);
        shapes.setNsPrefix("schema", "http://schema.org/")
        shapes.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
        shapes.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        shapes.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
        shapes.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")

        shapes.setNsPrefix("suave", "http://www.metacontrol.org/suave#")
        shapes.setNsPrefix("tomasys", "http://metacontrol.org/tomasys#")
        shapes.setNsPrefix("obo", "http://purl.obolibrary.org/obo/")



        // add shape axioms for all the axioms
        val triples = RDFDataMgr.loadDataset(maskFile).defaultModel
        var id = 0
        for (axiom in triples.listStatements()) {
            println(axiom)
            val shape = AxiomToShape(axiom, id)
            shape.forEach {shapes.add(it) }
            id += 1
        }

    }

    fun AxiomToShape(axiom: Statement, id : Int) : Set<Statement> {
        val shapeNode = shapes.getResource(prefixSchema+"shape$id")
        val setShape : MutableSet<Statement> = mutableSetOf()
        setShape.add(shapes.createStatement(
            shapeNode,
            typeProp,
            nodeShape
        ))

        setShape.add(shapes.createStatement(
            shapeNode,
            targetNode,
            axiom.subject
        ))

        val propertyNode = shapes.createResource("propNode$id")

        setShape.add(shapes.createStatement(
            shapeNode,
            shPropertyProperty,
            propertyNode
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shPathProperty,
            axiom.predicate
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shMinCountProperty,
            shapes.createTypedLiteral("1", "http://www.w3.org/2001/XMLSchema#integer")
        ))

        setShape.add(shapes.createStatement(
            propertyNode,
            shHasValueProperty,
                axiom.`object`
        ))


        return  setShape.toSet()
    }

    // writes shapes to file
    fun saveShapes(folderName: String, targetFile : String) {
        Files.createDirectories(Paths.get(folderName))
        val path = "$folderName/$targetFile.ttl"
        RDFDataMgr.write(File(path).outputStream(), shapes, Lang.TURTLE)
    }
}