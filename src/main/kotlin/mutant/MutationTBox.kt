package mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Statement
import randomGenerator


//removes one (random) subclass axiom       // val m = Mutator
// only overrides the method to select the candidate axioms from super class
class RemoveSubclassMutation(model: Model, verbose : Boolean) : RemoveAxiomMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val l = model.listStatements().toList()
        val candidates = l.toMutableSet()
        for (s in l) {
            // select statements that are not subClass relations
            if (!s.predicate.toString().matches(".*#subClassOf$".toRegex())) {
                candidates.remove(s)
            }
        }
        return filterRelevantPrefixes(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        val c = _config as SingleStatementConfiguration
        assert(c.getStatement().predicate.toString().matches(".*#subClassOf$".toRegex()))
        super.setConfiguration(_config)
    }
}

// removes one part of an "AND" in a logical axiom
class CEUAMutation(model: Model, verbose: Boolean): ReplaceNodeInAxiomMutation(model, verbose)   {
    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().isNotEmpty()
    }
    // selects names of nodes that should be removed / replaced by owl:Thing
    private fun getCandidates(): List<Pair<String, Statement>> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:intersectionOf ?b. " +
                "?b (rdfs:rest)* ?a." +
                "?a rdfs:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<Pair<String, Statement>>()
        for(r in res){
            val y = r.get("?y")
            val a = r.get("?a")
            val axiom = model.createStatement(a.asResource(),rdfFirst, y)
            println("$a $y $axiom")
            ret += Pair(y.toString(), axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        val con = _config as SingleStatementConfiguration
        val c = DoubleStringAndStatementConfiguration(
            con.getStatement().`object`.toString(),
            owlThing.toString(),
            con.getStatement())

        super.setConfiguration(_config)
    }

    override fun createMutation() {
        if (!hasConfig) {
            val (oldNode, axiom) = getCandidates().random(randomGenerator)
            val c = DoubleStringAndStatementConfiguration(
                oldNode,
                owlThing.toString(),
                axiom)
            super.setConfiguration(c)   // set configuration for upper class
        }
        super.createMutation()
    }
}

// removes one part of an "OR" in a logical axiom
class CEUOMutation(model: Model, verbose: Boolean): Mutation(model, verbose)   {

    // selects names of nodes that should be removed / replaced by owl:Nothing
    private fun getCandidates(): List<String> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:unionOf ?a1. " +
                "?a1 (rdfs:rest)* / rdfs:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<String>()
        for(r in res){
            val y = r.get("?y").toString()
            println(y)
            ret += y
        }
        return ret.sorted()
    }
}
