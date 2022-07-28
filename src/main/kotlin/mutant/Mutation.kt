package mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.StmtIterator
import kotlin.random.Random

abstract class Mutation(val model: Model, val verbose : Boolean) {
    abstract fun isApplicable() : Boolean
    abstract fun applyCopy() : Model
}

class RemoveAxiomMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {
    private fun getCandidates(): StmtIterator {
        return model.listStatements()
    }

    override fun isApplicable(): Boolean {
        return getCandidates().hasNext()
    }

    override fun applyCopy(): Model {
        val m = ModelFactory.createDefaultModel()
        val l = getCandidates().toList()
        val pos = Random.Default.nextLong(model.size()).toInt()
        if(verbose) println("removing: "+ l[pos])
        l.removeAt(pos)
        l.forEach { m.add(it) }
        return m
    }
}

class AddInstanceMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {
    private fun getCandidates(): List<String> {

        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n SELECT * WHERE { ?x a owl:Class. }"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        var ret = listOf<String>()
        for(r in res){
            val candidate = r.get("?x").toString()
            //first condition removes rdf: owl: and other standard prefixes, second condition matches on blank nodes
            if(candidate.startsWith("http://www.w3.org") || candidate.matches("(\\d|\\w){8}-(\\d|\\w){4}-(\\d|\\w){4}-(\\d|\\w){4}-(\\d|\\w){12}".toRegex()))
                continue
            ret = ret + candidate
        }
        return ret
    }

    override fun isApplicable(): Boolean {
        return getCandidates().isNotEmpty()
    }

    override fun applyCopy(): Model {
        val m = ModelFactory.createDefaultModel()
        model.listStatements().forEach { m.add(it) }
        val s = m.createStatement(
            m.createResource("inner:asd"+Random.nextInt(0,Int.MAX_VALUE)),
            m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            getCandidates().random())
        if(verbose) println("adding: $s")
        m.add(s)
        return m
    }
}