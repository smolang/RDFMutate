package mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import kotlin.random.Random

abstract class Mutation(val model: Model, val verbose : Boolean) {
    var hasConfig : Boolean = false
    open var config : MutationConfiguration? = null

    // constructor that creates mutation with configuration
    constructor(model: Model, _config: MutationConfiguration, verbose : Boolean) : this(model, verbose) {
        setConfiguration(_config)
    }

    abstract fun isApplicable() : Boolean
    abstract fun applyCopy() : Model

    open fun setConfiguration(_config : MutationConfiguration) {
        hasConfig = true
        config = _config
    }

    fun deleteConfiguration() {
        hasConfig = false
        config = null
    }

    fun deleteAxiom(s : Statement) : Model {
        val m = ModelFactory.createDefaultModel()
        if(verbose) println("removing: $s")
        // copy all statements that are not s
        model.listStatements().forEach {
            if (s != it) m.add(it)}
        return m
    }

    fun addAxiom(s : Statement) : Model {
        val m = ModelFactory.createDefaultModel()
        // copy all statements
        model.listStatements().forEach { m.add(it)}
        if(verbose) println("adding: $s")
        m.add(s)
        return m
    }

}

open class RemoveAxiomMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {

    open fun getCandidates(): List<Statement> {
        return model.listStatements().toList()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        super.setConfiguration(_config)
    }

    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().any()
    }

    override fun applyCopy(): Model {
        val s =
            if (hasConfig) {
                assert(config is SingleStatementConfiguration)
                val c = config as SingleStatementConfiguration
                c.getStatement()
            }
            else
                getCandidates().random()
        return deleteAxiom(s)
    }
}


//removes one (random) subclass axiom       // val m = Mutator

// only overrides the method to select the candidate axioms from super class
class RemoveSubclassMutation(model: Model, verbose : Boolean) : RemoveAxiomMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val l = model.listStatements().toList()
        val candidates = l.toMutableList()
        for (s in l) {
            // select statements that are not subClass relations
            if (!s.predicate.toString().matches(".*#subClassOf$".toRegex())) {
                candidates.remove(s)
            }
        }
        return candidates
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        val c = _config as SingleStatementConfiguration
        assert(c.getStatement().predicate.toString().matches(".*#subClassOf$".toRegex()))
        super.setConfiguration(_config)
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
        return hasConfig || getCandidates().isNotEmpty()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        super.setConfiguration(_config)
    }

    override fun applyCopy(): Model {
        val m = ModelFactory.createDefaultModel()
        val OWLClass =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource().toString()
            }
            else
                getCandidates().random()
        val instanceName =
            if (hasConfig && config is StringAndResourceConfiguration) {
                val c = config as StringAndResourceConfiguration
                c.getString()
            }
            else
                "inner:asd"+Random.nextInt(0,Int.MAX_VALUE)
        // create new "type" relation for the individual and the selected class
        val s = m.createStatement(
            m.createResource(instanceName),
            m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            m.createResource(OWLClass))

        return addAxiom(s)
    }
}

class AddRelationMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {


    private fun getCandidates() : List<String> {
        val cand = ArrayList<String>()
        val l = model.listStatements().toList()
        val ignore = ArrayList<String>()
        ignore.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
        ignore.add("http://www.w3.org/2000/01/rdf-schema#subClassOf")
        for (s in l) {
            val p = s.predicate.toString()
            // select relations that should not be ignored
            TODO("this check needs to be improved to also include other critical URIs")
            if (!ignore.contains(p) && !p.startsWith("http://www.w3.org/2002/07/owl"))
                // check if relation is already in
                if (!cand.contains(p))
                    cand.add(p)
        }
        return cand
    }
    override fun isApplicable(): Boolean {
        return getCandidates().any()
    }

    override fun applyCopy(): Model {
        print("cand for predicates" + getCandidates().toString())
        TODO("Not yet implemented")
        // should use AddAxiomMutation somehow...
    }

}

class AddAxiomMutation(model: Model, verbose: Boolean) : Mutation(model, verbose) {
    override fun isApplicable(): Boolean {
        return hasConfig
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleStatementConfiguration)
        super.setConfiguration(_config)
    }

    override fun applyCopy(): Model {
        assert(config != null)
        val c = config as SingleStatementConfiguration
        val s = c.getStatement()
        return addAxiom(s)
    }

}
