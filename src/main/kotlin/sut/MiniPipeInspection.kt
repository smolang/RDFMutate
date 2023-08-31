package sut

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import java.io.File

class MiniPipeInspection {

    var input : Model? = null

    fun move (thing: Resource, goal : Resource) {
        TODO("delete all old positions, set new position")
    }

    fun nextTo(thing: Resource) : Set<Resource> {
        TODO("collect all things that are connected via 'nextTo' ")
    }

    fun isAt(thing: Resource) : Resource {
        TODO("return the FIRST thing you find where the thing is at")
    }

    fun visited(thing: Resource) : Boolean {
        TODO("return if the object has status visited")
    }

    fun animal(thing: Resource) : Boolean {
        TODO("return if te thing is classified as an animal --> use reasoner!")
    }

    fun infrastructure(thing: Resource) : Boolean {
        TODO("return if te thing is classified as an animal --> use reasoner!")
    }

    fun allInfrastructureInspected() : Boolean {
        TODO("check if all Infrastructure is marked as inspected")
    }

    fun readOntology(file : File) {
        TODO("read ontology")
    }

    fun doInspection() {
        TODO("implement inspection agorithm")
    }


}