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
    var removeSet : MutableSet<Statement> = hashSetOf()

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
    val datatypeProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#DatatypeProperty")
    val oneOfProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#oneOf")
    val namedInd : Resource = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectProp : Resource = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
    val owlClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Class")
    val decimalClass : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#decimal")
    val booleanClass : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#boolean")
    val xsdDecimal : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#decimal")
    val datatypeClass : Resource = model.createResource("http://www.w3.org/2000/01/rdf-schema#Datatype")

    val rdfListClass : Resource = model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
    val rdfFirst : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
    val rdfRest : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
    val rdfNil : Resource = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil")


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
        m.removeSet.forEach { removeSet.add(it) }
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
        for (axiom in removeSet) {
            if (axiom.predicate == emptyProp)
                removeSet.remove(axiom)
        }

        if(verbose) println("removing: axioms $removeSet")
        if(verbose) println("adding: axioms $addSet")


        // copy all statements that are not deleteSet
        model.listStatements().forEach {
            if (!removeSet.contains(it)) m.add(it)}

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
            if (s.`object`.isResource)
                N.add(s.`object`.asResource())
        }
        return N.toSet()
    }

    fun isOfType(i : Resource, t : Resource) : Boolean {
        val l = model.listStatements(SimpleSelector(i, typeProp, null as RDFNode?))
        for (s in l) {
            if (s.`object` == t) {
                return true
            }
        }
        return false
    }

    fun allOfType(t : Resource) : Set<Resource> {
        return model.listResourcesWithProperty(typeProp, t).toSet()
    }

    fun isOfInferredType(i : Resource, t : Resource) : Boolean {
        val l = infModel.listStatements(SimpleSelector(i, typeProp, null as RDFNode?))
        for (s in l) {
            if (s.`object` == t) {
                return true
            }
        }
        return false
    }

    fun allOfInferredType(t : Resource) : Set<Resource> {
        return infModel.listResourcesWithProperty(typeProp, t).toSet()
    }

    // iterate through the axioms to add and remove existing relations with same subject and predicate
    fun turnAdditionsToChanges() {
        // find existing relations and remove them
        for (axiom in addSet) {
            for (existingAxiom in model.listStatements(
                SimpleSelector(axiom.subject, axiom.predicate, null as RDFNode?)
            ))
                removeSet.add(existingAxiom)
        }
    }

    // returns all elements from the linked a list in the ontology that starts in "head"
    fun allElementsInList(head: Resource) : List<RDFNode> {
        // assure that element is really a list
        if (!model.contains(model.createStatement(
            head,
            typeProp,
            rdfListClass
        )))
            return listOf()
        else {
            val list : MutableList<RDFNode> = mutableListOf()
            // add all (i.e. one) current element
            for (element in model.listObjectsOfProperty(head, rdfFirst))
                list.add(element)

            // check if there is more list to come

            val rest = model.listObjectsOfProperty(head, rdfRest).toSet()
            if (rest.contains(rdfNil))
                // base case
                return list
            else {
                // recursive call
                val restElements = allElementsInList(rest.single().asResource())
                list.addAll(restElements)
                return list
            }
        }
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
        removeSet.add(s)
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
                                          rangeP : Set<RDFNode>) : Statement? {
        // generated axiom is part of ontology --> more effort on finding non-contained one
        // find all pairs of this relation that are already in ontology
        val containedPairs : MutableSet<Pair<Resource, RDFNode>> = hashSetOf()
        val candidatePairs : MutableSet<Pair<Resource, RDFNode>> = hashSetOf()

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
    // e.g. if p is functional or asymmetric
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
                    removeSet.add(it)
                }
            }
        }
        // TODO: similar check for asymmetric
    }

    private fun computeDomsObjectProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // only select individuals and according to range and domain
        // note: this is very restrictive, as usually, one could infer the class from the relation
        // our setting is more useful in a closed-world scenario

        assert(isOfType(p, objectProp))
        // compute domains

        val Ind = allOfType(namedInd)   // all individuals

        val domains = infModel.listStatements(SimpleSelector(p, domainProp, null as RDFNode?)).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        // compute range
        val ranges = infModel.listStatements(SimpleSelector(p, rangeProp, null as RDFNode?)).toSet()
        var rangeP : MutableSet<Resource> = Ind.toMutableSet()
        ranges.forEach {
            rangeP = rangeP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        return Pair(domainP, rangeP)
    }

    private fun computeDomsDataProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // only select individuals and according to range and domain
        // note: this is very restrictive, as usually, one could infer the class from the relation
        // our setting is more useful in a closed-world scenario

        assert(isOfType(p, datatypeProp))

        // compute domains

        val Ind = allOfType(namedInd)   // all individuals

        val domains = infModel.listStatements(SimpleSelector(p, domainProp, null as RDFNode?)).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        val ranges = model.listObjectsOfProperty(p, rangeProp).toSet()

        var rangeP : MutableSet<RDFNode> = hashSetOf()

        if (ranges.size != 1) {
            if (verbose)
                println("can not add data relation. Property ${p.localName} has no or more than one range provided")
        }
        else {
            val range = ranges.single()
            // check different classes of data properties, for which we can determine the domain
            if (range == booleanClass) {
                val trueNode = model.createResource("true")
                val falseNode = model.createResource("false")
                rangeP = hashSetOf(trueNode, falseNode)
            }
            else if (range == xsdDecimal) {
                // compute a random decimal number

                // 50% chance of having a negative number
                val sign =
                    if (Random.nextBoolean())
                        "-"
                    else
                        ""

                // the absolute value favours small numbers --> 1/x distribution
                // e.g. probability of having 0 as leading number is 50%
                val beforeKomma = ((1/(-Random.nextDouble(-1.0, 1.0) + 1.0))).toInt()
                val data = "$sign$beforeKomma.${Random.nextInt(0,1000)}"
                rangeP = hashSetOf(model.createTypedLiteral(data, xsdDecimal.toString()))

            }
            else if (isOfType(range.asResource(), datatypeClass)) {
                // check if it is a list of statements
                val oneOf = model.listObjectsOfProperty(
                    range.asResource(),
                    oneOfProp
                ).toSet()
                if (oneOf.size != 1){
                    if (verbose)
                        println("can not add data relation. Property ${p.localName} is marked as 'Datatype' but does" +
                                " not provide one 'oneOf'-relation ")
                }
                else {
                    val list = oneOf.single()
                    if (!isOfType(list.asResource(), rdfListClass)){
                        println("can not add data relation. Property ${p.localName} has not a list as 'oneOf'.")
                    }
                    else {
                        // collect elements of list
                        rangeP = allElementsInList(list.asResource()).toMutableSet()
                    }
                }

            }
            else {
                if (verbose)
                    println("the range of datatype property ${p.localName} is not implemented yet. No axiom is added")
            }
        }

        return Pair(domainP, rangeP)
    }

    open fun computeDomsProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // build sets for the elements that are in the domain and range of the property
        val domainP : Set<Resource>
        val rangeP : Set<RDFNode>

        val Ind = allOfType(namedInd)   // all individuals

        // is property an ObjectProperty?
        if (isOfType(p, objectProp)) {
            val (d, r) = computeDomsObjectProp(p)
            domainP = d
            rangeP = r
        }
        else if (isOfType(p, datatypeProp)) {
            val (d, r) = computeDomsDataProp(p)
            domainP = d
            rangeP = r
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

        return Pair(domainP, rangeP)
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

        val (domainP, rangeP) = computeDomsProp(p)

        // check, if there are candidates to add this relation
        if (domainP.any() && rangeP.any()) {
            var axiomCand = model.createStatement(domainP.random(), p, rangeP.random())

            // test if axiom exists
            // try 10 times to find an axiom that does not exist, as this often works and it is fast
            var i = 0
            while (!validAddition(axiomCand) && i < 10) {
                // find new axiom
                axiomCand = model.createStatement(domainP.random(), p, rangeP.random())
                i += 1
            }

            // test if a non-contained axiom was found
            val axiom =
                if (!validAddition(axiomCand))
                    // use expensive search for a valid axiom in case no valid one was found so far
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
        }
        super.createMutation()
    }
}

// similar to adding a relation, but all existing triples with this subject ond predicate are deleted
open class ChangeRelationMutation(model: Model, verbose: Boolean) : AddRelationMutation(model, verbose) {

    override fun computeDomsProp(p: Property): Pair<Set<Resource>, Set<RDFNode>> {
        // use all individuals as domain that already have such an outgoing relation
        val domainP = model.listResourcesWithProperty(p).toSet()
        // use range from super method
        val (_, rangeP) = super.computeDomsProp(p)
        return Pair(domainP, rangeP)
    }

    override fun createMutation() {
        // create the mutation as usual (i.e. adding a new triple)
        super.createMutation()

        // find existing relations and remove them
        turnAdditionsToChanges()
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
        //val l = model.listStatements().toList()
        //var found = false
        assert(isOfType(p.asResource(), objectProp))
       // for (s in l) {
       //     if (s.subject == p && s.predicate == typeProp && s.`object` == objectProp)
       //         found = true
       // }
        //assert(found)
        super.setConfiguration(_config)
    }
}

open class ChangeObjectPropertyMutation(model: Model, verbose: Boolean) : AddObjectPropertyMutation(model, verbose) {
    override fun createMutation() {
        // create the mutation as usual (i.e. adding a new triple)
        super.createMutation()

        // find existing relations and remove them
        turnAdditionsToChanges()
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
            if (s.`object`.isResource)
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
            removeSet.add(it)
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

