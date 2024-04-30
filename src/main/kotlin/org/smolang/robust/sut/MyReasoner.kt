package org.smolang.robust.sut

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.reasoner.ReasonerRegistry

class MyReasoner(model : Model) {
    val reasoner = ReasonerRegistry.getOWLReasoner()
    val inf = ModelFactory.createInfModel(reasoner, model)

    val typeProp = inf.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

    fun allIndividuals(OWLclass : Resource) : List<Resource>{
        var ret = listOf<Resource>()
        for (axiom in inf.listStatements())
            if (axiom.`object`.equals(OWLclass) && axiom.predicate.equals(typeProp))
                ret = ret + axiom.subject
        return ret
    }

    fun allOutgoingRelations(OWLindivid : Resource) : List<Statement>{
        var ret = listOf<Statement>()
        for (axiom in inf.listStatements())
            if (axiom.subject.equals(OWLindivid))
                ret = ret + axiom
        return ret
    }

    fun getInf() : Model {
        return inf
    }


}