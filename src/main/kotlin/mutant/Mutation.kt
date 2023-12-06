package mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import kotlin.random.Random

open class Mutation(var model: Model, val verbose : Boolean) {
    var hasConfig : Boolean = false
    open var config : MutationConfiguration? = null
    var createdMutation : Boolean = false

    // set of axioms to add or delete in this mutation
    var addSet : MutableSet<Statement> = hashSetOf()
    var deleteSet : MutableSet<Statement> = hashSetOf()

    val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()

    // define some properties / resources that are use all the time
    val typeProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val domainProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#domain")
    val rangeProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#range")
    val subClassProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")
    val funcProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#FunctionalProperty")
    val namedInd : Resource = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectProp : Resource = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
    val owlClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Class")


    // constructor that creates mutation with configuration
    constructor(model: Model, _config: MutationConfiguration, verbose : Boolean) : this(model, verbose) {
        this.setConfiguration(_config)
    }

    open fun isApplicable() : Boolean {
        return true
    }

    // applies the mutation and creates a copy
    fun applyCopy() : Model {
        createMutation()
        assert(createdMutation)
        return addDeleteAxioms()
    }

    // selects that add- and delete-set of axioms
    // sets the flag to true
    open fun createMutation() {
        createdMutation = true
    }

    // extracts the changes from a given mutation to perform the same changes
    // i.e. adds the changes to the existing changes
    fun mimicMutation(m : Mutation) {
        assert(m.createdMutation)
        m.addSet.forEach { addSet.add(it) }
        m.deleteSet.forEach { deleteSet.add(it) }
    }

    open fun setConfiguration(_config : MutationConfiguration) {
        hasConfig = true
        config = _config
    }

    fun deleteConfiguration() {
        hasConfig = false
        config = null
    }

    // adds and deletes the axioms as specified in the according sets
    // creates a new model
    private fun addDeleteAxioms() : Model {
        val m = ModelFactory.createDefaultModel()
        if(verbose) println("removing: axioms $deleteSet")
        if(verbose) println("adding: axioms $addSet")

        // copy all statements that are not deleteSet
        model.listStatements().forEach {
            if (!deleteSet.contains(it)) m.add(it)}
        addSet.forEach {
            m.add(it)
        }
        return m
    }


    fun allNodes() : Set<Resource> {
        val l = model.listStatements()
        val N : MutableSet<Resource> = hashSetOf()
        for (s in l) {
            // select statements that are not subClass relations
            N.add(s.subject)
            N.add(s.`object`.asResource())
        }
        return N.toSet()
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

    fun allOfType(t : Resource) : Set<Resource> {
        return model.listResourcesWithProperty(typeProp, t).toSet()
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

    fun allOfInferredType(t : Resource) : Set<Resource> {
        val infModel = ModelFactory.createInfModel(reasoner, model)
        return infModel.listResourcesWithProperty(typeProp, t).toSet()
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

    override fun createMutation() {
        val s =
            if (hasConfig) {
                assert(config is SingleStatementConfiguration)
                val c = config as SingleStatementConfiguration
                c.getStatement()
            }
            else
                getCandidates().random()
        deleteSet.add(s)
        super.createMutation()
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

    override fun createMutation() {
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

        addSet.add(s)
        super.createMutation()
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

    override fun createMutation() {
        // select candidate
        val prop =
            if (hasConfig) {
                when (config) {
                    is SingleResourceConfiguration -> {
                        val c = config as SingleResourceConfiguration
                        c.getResource()
                    }

                    is SingleStatementConfiguration -> {
                        val c = config as SingleStatementConfiguration
                        c.getStatement().predicate
                    }

                    else -> model.createResource()
                }
            }
            else
                getCandidates().random()

        val p = model.getProperty(prop.toString())

        // check for restrictions of adding

        // bild sets for the elements that are in the domain and range of the property
        val domainP : Set<Resource>
        val rangeP : Set<Resource>

        val Ind = allOfType(namedInd)   // all individuals

        // is property an ObjectProperty?
        if (isOfType(p, objectProp)) {
            // only select individuals and according to range and domain

            // check, if a domain exists
            val dom = model.getProperty(p, domainProp)
            domainP =
                if(dom != null)
                    allOfInferredType(dom.`object`.asResource()).intersect(Ind)
                else
                    Ind
            // check if a range exits
            val ran = model.getProperty(p, rangeProp)
            rangeP =
                if(ran != null)
                    allOfInferredType(ran.`object`.asResource()).intersect(Ind)
                else
                    Ind

        }
        // is property type property?
        else if (p == typeProp){
            // let's restrict ourselves to add type relations between individuals and classes
            domainP = Ind
            rangeP = allOfType(owlClass)
        }
        else if (p == subClassProp){
            domainP = allOfType(owlClass)
            rangeP = allOfType(owlClass)
        }
        else if (p == domainProp || p == rangeProp){
            domainP = allOfType(objectProp)
            rangeP = allOfType(owlClass)
        }
        else {
            // other special cases are not considered yet --> add relation between any two nodes
            domainP = allNodes()
            rangeP = allNodes()
        }



        val axiom = model.createStatement(domainP.random(), p, rangeP.random())

        // check if property is functional: if yes --> delete old edge
        if (isOfType(p, funcProp)){
            // delete outgoing relations, so that relation remains functional
            val del = model.listStatements(
                SimpleSelector(axiom.subject.asResource(), p, null as RDFNode?)).toList()
            del.forEach {
                deleteSet.add(it)
            }
        }
        // TODO: similar check for irreflexiv, asymmetric

        addSet.add(axiom)
        super.createMutation()
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
            when (_config) {
                is SingleResourceConfiguration -> {
                    _config.getResource()
                }

                is SingleStatementConfiguration -> {
                    _config.getStatement().predicate
                }

                else -> model.createResource()
            }

        // check that p is really an ObjectProperty
        val l = model.listStatements().toList()
        var found = false
        for (s in l) {
            if (s.subject == p && s.predicate == typeProp && s.`object` == objectProp)
                found = true
        }
        assert(found)
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

    override fun createMutation() {
        assert(config != null)
        val c = config as SingleStatementConfiguration
        val s = c.getStatement()
        addSet.add(s)
        super.createMutation()
    }
}

open class RemoveNodeMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {

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

    override fun createMutation() {
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

        delete.forEach {
            deleteSet.add(it)
        }

        super.createMutation()
    }
}

class RemoveIndividualMutation(model: Model, verbose : Boolean) : RemoveNodeMutation(model, verbose) {
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

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        // assert that the resource is really an individual
        val ind = (_config as SingleResourceConfiguration).getResource()
        assert(isOfType(ind, namedInd))
        super.setConfiguration(_config)
    }


}

