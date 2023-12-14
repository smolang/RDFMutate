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

    // some objects to work with the inferred model
    val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()

    // using this infModel assumes that "model" never changed
    // i.e. it is the inferred model at the time of initialisation
    val infModel = ModelFactory.createInfModel(reasoner, model)



    // define some properties / resources that are use all the time
    val typeProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val domainProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#domain")
    val rangeProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#range")
    val subClassProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")
    val subPropertyProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
    val funcProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#FunctionalProperty")
    val irreflexivProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#IrreflexiveProperty")
    val namedInd : Resource = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectProp : Resource = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
    val owlClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Class")

    // empty Axiom
    val emptyProp : Property = model.createProperty("emptyAxiomProp")
    val emptyAxiom : Statement = model.createStatement(model.createResource(), emptyProp, model.createResource())

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

        // clean the sets from empty axioms
        for (axiom in addSet) {
            if (axiom.predicate == emptyProp)
                addSet.remove(axiom)
        }
        for (axiom in deleteSet) {
            if (axiom.predicate == emptyProp)
                deleteSet.remove(axiom)
        }

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
        val l = infModel.listStatements()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.subject == i && s.predicate == typeProp && s.`object` == t) {
                return true
            }
        }
        return false
    }

    fun allOfInferredType(t : Resource) : Set<Resource> {
        return infModel.listResourcesWithProperty(typeProp, t).toSet()
    }


}

open class RemoveAxiomMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {

    open fun getCandidates(): Set<Statement> {
        return model.listStatements().toSet()
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
    override fun getCandidates(): Set<Statement> {
        val l = model.listStatements().toList()
        val candidates = l.toMutableSet()
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

    // checks if the addition of the axiom is valid,
    // i.e. not already contained and respects irreflexivity
    private fun validAddition(axiom : Statement) : Boolean {
        val p = axiom.predicate

        if (model.contains(axiom))
            return false

        if (isOfType(p, irreflexivProp)) {
            // check if the new axiom is not reflexiv
            if (axiom.subject == axiom.`object`.asResource())
                return false
        }
        return true
    }

    private fun costlySearchForValidAxiom(p : Property,
                                          domainP : Set<Resource>,
                                          rangeP : Set<Resource>) : Statement? {
        // generated axiom is part of ontology --> more effort on finding non-contained one
        // find all pairs of this relation that are already in ontology
        val containedPairs : MutableSet<Pair<Resource, Resource>> = hashSetOf()
        val candidatePairs : MutableSet<Pair<Resource, Resource>> = hashSetOf()

        // iterate over statements in ontology with this property
        for (s in model.listStatements(SimpleSelector(null as Resource?, p, null as RDFNode?))) {
            containedPairs.add(Pair(s.subject, s.`object`.asResource()))
        }

        // iterate over candidate pairs of statements
        domainP.forEach { RDFsubject ->
            rangeP.forEach{ RDFobject ->
                candidatePairs.add(Pair(RDFsubject, RDFobject))
            }
        }

        // remove existing pairs from candidate pairs
        candidatePairs.removeAll(containedPairs)

        // filter, to only have the pairs whose addition is valid
        val filteredPairs = candidatePairs.filter { (RDFsubject, RDFobject) ->
            validAddition(model.createStatement(RDFsubject, p, RDFobject))
        }

        val randomPair = filteredPairs.randomOrNull()

        if (randomPair != null) {
            val (randSubject, randObject) = randomPair
            return model.createStatement(randSubject, p, randObject)
        }
        else
            println("we could not add a valid relation with property '${p.localName}'")

        return null
    }

    // adds axioms to the delete set if necessary
    // e.g. if p is functional or assymmetric
    private fun setRepairs(p : Property, axiom : Statement) {
        if (isOfInferredType(p, funcProp)) {
            // delete outgoing relations, so that relation remains functional
            // i.e. we are quite restrictive here, this makes only sense in the case of a unique name assumption
            // do this for all superProps of the property
            for (superPropAxiom in infModel.listStatements(SimpleSelector(p, subPropertyProp, null as RDFNode?))) {
                val superProp = model.createProperty(superPropAxiom.`object`.toString())
                model.listStatements(
                    SimpleSelector(axiom.subject.asResource(), superProp, null as RDFNode?)
                ).forEach {
                    deleteSet.add(it)
                }
            }
        }
        // TODO: similar check for asymmetric
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

        // build sets for the elements that are in the domain and range of the property
        val domainP : Set<Resource>
        val rangeP : Set<Resource>

        val Ind = allOfType(namedInd)   // all individuals

        // is property an ObjectProperty?
        if (isOfType(p, objectProp)) {
            // only select individuals and according to range and domain
            // note: this is very restrictive, as usually, one could infer the class from the relation
            // our setting is more useful in a closed-world scenario

            // compute domains
            val domains = infModel.listStatements(SimpleSelector(p, domainProp, null as RDFNode?)).toSet()
            var allInDom : MutableSet<Resource> = Ind.toMutableSet()
            domains.forEach {
                allInDom = allInDom.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
            }
            domainP = allInDom

            // compute range
            val ranges = infModel.listStatements(SimpleSelector(p, rangeProp, null as RDFNode?)).toSet()
            var allInRange : MutableSet<Resource> = Ind.toMutableSet()
            ranges.forEach {
                allInRange = allInRange.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
            }
            rangeP = allInRange
        }

        // TODO: is property data property?

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


        var i = 0
        var axiomCand = model.createStatement(domainP.random(), p, rangeP.random())

        // test if axiom exists
        // try 10 times to find an axiom that does not exist, as this often works is is fast
        while (!validAddition(axiomCand) && i < 10)
        {
            // find new axiom
            axiomCand = model.createStatement(domainP.random(), p, rangeP.random())
            i += 1
        }

        // test if a non-contained axiom was found
        val axiom =
            if (!validAddition(axiomCand))
                costlySearchForValidAxiom(p, domainP, rangeP)
            else
                axiomCand


        // if the selected axiom is not contained in ontology and the addition is valid --> we add it
        // otherwise: we do not add or delete anything
        if (axiom != null && validAddition(axiom)) {
            addSet.add(axiom)
            // add elements to repair set, s.t. obvious inconsistencies are circumvented
            setRepairs(p, axiom)
        }
        super.createMutation()
    }
}

open class RemoveObjectPropertyMutation(model: Model, verbose : Boolean) : RemoveAxiomMutation(model, verbose) {
    override fun getCandidates(): Set<Statement> {
        val allProps = allOfType(objectProp)
        val candidates : MutableSet<Statement> = hashSetOf()
        allProps.forEach {
            candidates.addAll(model.listStatements(
                SimpleSelector(null as Resource?, model.getProperty(it.toString()), null as RDFNode?)
            ).toSet())
        }
        return candidates.toSet()
    }

    override fun setConfiguration(_config: MutationConfiguration) {
        assert(_config is SingleResourceConfiguration)
        when (_config) {
            is SingleResourceConfiguration -> {
                // select a random element to delete
                val p = model.getProperty(_config.getResource().toString())
                val cand = model.listStatements(null as Resource?, p, null as RDFNode?).toSet()

                val axiom =
                    if (cand.any())
                        cand.random()
                    else {
                        if (verbose)
                            println("no relation with this property exists / can be deleted")
                        emptyAxiom
                    }
                super.setConfiguration(
                    SingleStatementConfiguration(axiom)
                )
            }
            is SingleStatementConfiguration -> {
                super.setConfiguration(_config)
            }
        }

    }
}
open class AddObjectPropertyMutation(model: Model, verbose: Boolean) : AddRelationMutation(model, verbose) {
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

