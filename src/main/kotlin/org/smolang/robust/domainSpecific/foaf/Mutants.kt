package org.smolang.robust.domainSpecific.foaf

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.update.UpdateFactory
import org.apache.jena.vocabulary.RDF
import org.smolang.robust.mutant.Mutation

open class FOAFMutant(model: Model) : Mutation(model){
    companion object {
        var i = 0
    }
    fun getNextPerson() : Pair<Resource,String> {
        val personUri = "http://smolang.org/testing/person$i"
        val personName = "Peter$i Müller"
        i++
        return Pair(model.createResource(personUri), personName)
    }
}

class Mutant(model : Model) : FOAFMutant(model) {

    override fun isApplicable(): Boolean {
        return true
    }
    override fun createMutation() {
        super.createMutation()
        val pp = getNextPerson();
        // Create statements
        val typeStmt = model.createStatement(pp.first, RDF.type, FOAF.Person)
        val nameStmt = model.createStatement(
            pp.first, FOAF.name,
            model.createLiteral(pp.second, "de")
        )

        // Add statements to model
        addSet.add(typeStmt)
        addSet.add(nameStmt)
    }
}

class SPARQLMutant(model: Model) : FOAFMutant(model) {
    override fun isApplicable(): Boolean {
        return true
    }
    override fun createMutation() {
        super.createMutation()


        val queryWithPrefixes = """
            DELETE DATA { ?a <${FOAF.member}> ?b. }
            INSERT DATA { ?a <${FOAF.member}> ?c. }
            WHERE { 
            SELECT ?a ?b ?c WHERE {
              ?a <${FOAF.member.uri}> ?b.
              ?c <${RDF.type.uri}> <${FOAF.Organization}>.
              FILTER NOT (?a <${FOAF.member.uri}> ?c.)
              } LIMIT 1
             }
        """.trimIndent()
        updateRequestList.add(UpdateFactory.create(queryWithPrefixes))
    }
}