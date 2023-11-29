package mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.reasoner.ReasonerRegistry
import kotlin.random.Random

abstract class Mutation(val model: Model, val verbose : Boolean) {
    var hasConfig : Boolean = false
    open var config : MutationConfiguration? = null

    val reasoner = ReasonerRegistry.getOWLReasoner()

    // define some properties that are use all the time
    val typeProp = model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val namedInd = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectProp = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")

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

    fun deleteAxioms(l : List<Statement>) : Model {
        val m = ModelFactory.createDefaultModel()
        if(verbose) println("removing: axioms " + l.toString())
        // copy all statements that are not s
        model.listStatements().forEach {
            var delete = false
            for (s in l)
                if (s == it) delete = true
            if (!delete) m.add(it)}
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

    fun isOfType(i : Resource, t : Resource) : Boolean {
        val l = model.listStatements()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.subject == i && s.predicate == typeProp && s.`object` == t) {
                return true
            }
        }
        return false
    }

    fun isOfInferredType(i : Resource, t : Resource) : Boolean {
        val l = ModelFactory.createInfModel(reasoner, model).listStatements()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.subject == i && s.predicate == typeProp && s.`object` == t) {
                return true
            }
        }
        return false
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

open class AddRelationMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {
    open fun getCandidates() : List<Resource> {
        val cand = ArrayList<Resource>()
        val l = model.listStatements().toList()
        for (s in l) {
            val p = s.predicate
            if (!cand.contains(p))
                cand.add(p)
        }
        return cand
    }
    override fun isApplicable(): Boolean {
        return getCandidates().any()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration || _config is SingleStatementConfiguration)
        super.setConfiguration(_config)
    }

    override fun applyCopy(): Model {
        print("cand for predicates" + getCandidates().toString())

        // select candidate
        val p =
            if (hasConfig) {
                if (config is SingleResourceConfiguration) {
                    val c = config as SingleResourceConfiguration
                    c.getResource()
                }
                else if (config is SingleStatementConfiguration){
                    val c = config as SingleStatementConfiguration
                    c.getStatement().predicate
                }
                else
                    model.createResource()
            }
            else
                getCandidates().random()

        // check for restrictions of adding
        TODO("continue here")
        // is property an ObjectProperty?
        // range? domain?

        // is property type property?
        // is property subClassProperty?


        return model
        // should use AddAxiomMutation somehow...
    }
}


class AddObjectProperty(model: Model, verbose: Boolean) : AddRelationMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand = ArrayList<Resource>()
        val l = model.listStatements().toList()
        for (s in l) {
            if (s.predicate == typeProp && s.`object` == objectProp)
                cand.add(s.subject)
        }
        return cand
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        val p =
            if (_config is SingleResourceConfiguration) {
                val c = _config as SingleResourceConfiguration
                c.getResource()
            }
            else if (_config is SingleStatementConfiguration){
                val c = _config as SingleStatementConfiguration
                c.getStatement().predicate
            }
            else
                model.createResource()

        // check that p is really an ObjectProperty
        val l = model.listStatements().toList()
        var found = false
        for (s in l) {
            if (s.subject == p && s.predicate == typeProp && s.`object` == objectProp)
                found = true
        }
        assert(found)
        println("everything good")
        super.setConfiguration(_config)
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

open class RemoveNode(model: Model, verbose : Boolean) : Mutation(model, verbose) {

    open fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates : MutableSet<Resource> = hashSetOf()
        for (s in l) {
            candidates.add(s.subject)
            candidates.add(s.`object`.asResource())
        }
        return candidates.toList()
    }

    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().any()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        super.setConfiguration(_config)
    }

    override fun applyCopy(): Model {
        // select an resource to delete
        val res =
            if (hasConfig) {
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource()
            }
            else
                getCandidates().random()

        // select all axioms that contain the resource
        val l = model.listStatements().toList().toMutableList()
        val delete = ArrayList<Statement>()
        for (s in l) {
            if (s.subject == res || s.predicate == res || s.`object` == res) {
                delete.add(s)
            }
        }

        return deleteAxioms(delete)
    }
}

class RemoveIndividual(model: Model, verbose : Boolean) : RemoveNode(model, verbose) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.predicate == typeProp && s.`object` == namedInd) {
                candidates.add(s.subject)
            }
        }
        return candidates
    }
}

