package org.smolang.robust.mutant.DefinedMutants

import org.apache.jena.rdf.model.*
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.*
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.ComplexTermParser

open class RemoveStatementMutation(model: Model) : Mutation(model) {

    open fun getCandidates(): List<Statement> {
        val list = model.listStatements().toList()
        val f = filterMutableStatements(list)
        return f.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        super.setConfiguration(config)
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
                getCandidates().randomOrNull(randomGenerator)

        if (s != null)
            removeSet.add(s)

        super.createMutation()
    }
}

open class AddRelationMutation(model: Model) : Mutation(model) {
   // relations that should not be randomly selected, as they will lead to problems in the schema
    private val excludeP : List<Property> = listOf(
       //typeProp,
       model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first"),
       model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
    )
    //val exclude
    private val excludePrefixes : List<String> = listOf(
        "http://www.w3.org/2002/07/owl",
        "http://www.w3.org/2003/11/swrl",
        "http://www.w3.org/2003/11/swrlb#",
        "http://swrl.stanford.edu/ontologies/3.3/swrla.owl#"
    )

    private  fun excludePrefix(r : Resource) : Boolean {
        val stringName = r.toString()
        for (prefix in excludePrefixes)
            if (stringName.startsWith(prefix))
                return true
        return false
    }
    private fun excludeProperty(p : Property) : Boolean {
        return excludeP.contains(p) || excludePrefix(p.asResource())
    }

    open fun getCandidates() : List<Resource> {
        val cand : MutableList<Resource> = mutableListOf()
        val l = model.listStatements().toList()
        for (s in l) {
            val p = s.predicate
            if (!cand.contains(p) && !excludeProperty(p))
                cand.add(p)
        }
        return cand.sortedBy { it.toString() }
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun isApplicable(): Boolean {
        return getCandidates().any()
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration || config is SingleStatementConfiguration)
        super.setConfiguration(config)
    }

    // checks if the addition of the axiom is valid,
    // i.e. not already contained and respects irreflexivity
    private fun validAddition(axiom : Statement) : Boolean {
        val p = axiom.predicate

        if (model.contains(axiom))
            return false

        if (isOfType(p, OWL.IrreflexiveProperty)) {
            // check if the new axiom is not reflexive
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
        for (s in model.listStatements(null as Resource?, p, null as RDFNode?)) {
            if (s.`object`.isResource)
                containedPairs.add(Pair(s.subject, s.`object`.asResource()))
        }

        // iterate over candidate pairs of statements
        domainP.forEach { rdfSubject ->
            rangeP.forEach{ rdfObject ->
                candidatePairs.add(Pair(rdfSubject, rdfObject))
            }
        }

        // remove existing pairs from candidate pairs
        candidatePairs.removeAll(containedPairs)

        // filter, to only have the pairs whose addition is valid
        val filteredPairs = candidatePairs.filter { (rdfSubject, rdfObject) ->
            validAddition(model.createStatement(rdfSubject, p, rdfObject))
        }

        val randomPair = filteredPairs.randomOrNull(randomGenerator)

        if (randomPair != null) {
            val (randSubject, randObject) = randomPair
            return model.createStatement(randSubject, p, randObject)
        }
        else
            mainLogger.info("we could not add a valid relation with property '${p.localName}'")

        return null
    }

    // adds axioms to the delete set if necessary
    // e.g. if p is functional or asymmetric
    private fun setRepairs(p : Property, axiom : Statement) {
        if (isOfInferredType(p, OWL.FunctionalProperty)) {
            // delete outgoing relations, so that relation remains functional
            // i.e. we are quite restrictive here, this makes only sense in the case of a unique name assumption
            // do this for all superProps of the property
            for (superPropAxiom in infModel.listStatements(p, RDFS.subPropertyOf, null as RDFNode?)) {
                val superProp = model.createProperty(superPropAxiom.`object`.toString())
                model.listStatements(
                    axiom.subject.asResource(), superProp, null as RDFNode?
                ).forEach {
                    removeSet.add(it)
                }
            }
        }
        // TODO: similar check for asymmetric
    }

    private fun computeDomainsObjectProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // only select individuals and according to range and domain
        // note: this is very restrictive, as usually, one could infer the class from the relation
        // our setting is more useful in a closed-world scenario

        assert(isOfType(p, OWL.ObjectProperty))
        // compute domains

        val Ind = allOfType(OWL.NamedIndividual)   // all individuals

        val domains = infModel.listStatements(p, RDFS.domain, null as RDFNode?).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        // compute range
        val ranges = infModel.listStatements(p, RDFS.range, null as RDFNode?).toSet()
        var rangeP : MutableSet<Resource> = Ind.toMutableSet()
        ranges.forEach {
            rangeP = rangeP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }
        return Pair(domainP, rangeP)
    }

    private fun computeDomainsDataProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // only select individuals and according to range and domain
        // note: this is very restrictive, as usually, one could infer the class from the relation
        // our setting is more useful in a closed-world scenario

        assert(isOfType(p, OWL.DatatypeProperty))

        // compute domains

        val Ind = allOfType(OWL.NamedIndividual)   // all individuals

        val domains = infModel.listStatements(p, RDFS.domain, null as RDFNode?).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        val ranges = model.listObjectsOfProperty(p, RDFS.range).toSet()

        var rangeP : MutableSet<RDFNode> = hashSetOf()

        if (ranges.size > 1) {
            mainLogger.warn("can not add data relation. Property ${p.localName} has more than one range provided")
        }
        else {
            val range =
                if (ranges.size == 1)
                    ranges.single()
                else {
                    val r = HashSet<RDFNode>()
                    r.add(XSD.xboolean)
                    r.add(XSD.xdouble)
                    r.add(XSD.decimal)
                    r.random(randomGenerator)
                }

            // check different classes of data properties, for which we can determine the domain
            if (range == XSD.xboolean) {
                val trueNode = model.createTypedLiteral("true", XSD.xboolean.toString())
                val falseNode = model.createTypedLiteral("false", XSD.xboolean.toString())
                rangeP = hashSetOf(trueNode, falseNode)
            }
            else if (range == XSD.decimal) {
                // compute a random decimal number

                // 50% chance of having a negative number
                val sign =
                    if (randomGenerator.nextBoolean())
                        "-"
                    else
                        ""

                // the absolute value favours small numbers --> 1/x distribution
                // e.g. probability of having 0 as leading number is 50%
                val beforeComma = ((1/(-randomGenerator.nextDouble(-1.0, 1.0) + 1.0))).toInt()
                val data = "$sign$beforeComma.${randomGenerator.nextInt(0,1000)}"
                rangeP = hashSetOf(model.createTypedLiteral(data, XSD.decimal.toString()))

            }
            else if (range == XSD.xdouble) {
                // compute a random double  number

                // 50% chance of having a negative number
                val sign =
                    if (randomGenerator.nextBoolean())
                        "-"
                    else
                        ""

                // the absolute value favours small numbers --> 1/x distribution
                // e.g. probability of having 0 as leading number is 50%
                val beforeComma = ((1/(-randomGenerator.nextDouble(-1.0, 1.0) + 1.0))).toInt()
                val data = "$sign$beforeComma.${randomGenerator.nextInt(0,1000)}"
                rangeP = hashSetOf(model.createTypedLiteral(data, XSD.xdouble.toString()))

            }
            else if (isOfType(range.asResource(), RDFS.Datatype)) {
                // check if it is a list of statements
                val oneOf = model.listObjectsOfProperty(
                    range.asResource(),
                    OWL.oneOf
                ).toSet()
                if (oneOf.size != 1){
                    mainLogger.warn("can not add data relation. Property ${p.localName} is marked as 'Datatype' but does" +
                                " not provide one 'oneOf'-relation ")
                }
                else {
                    val list = oneOf.single()
                    if (!isOfType(list.asResource(), RDF.List)){
                        mainLogger.warn("can not add data relation. Property ${p.localName} has not a list as 'oneOf'.")
                    }
                    else {
                        // collect elements of list
                        rangeP = ComplexTermParser().allElementsInList(model, list.asResource()).toMutableSet()
                    }
                }

            }
            else {
                mainLogger.warn("the range of datatype property ${p.localName} is not implemented yet. No axiom is added")
            }
        }

        return Pair(domainP, rangeP)
    }

    open fun computeDomainsProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        // build sets for the elements that are in the domain and range of the property
        val domainP : Set<Resource>
        val rangeP : Set<RDFNode>

        val Ind = allOfType(OWL.NamedIndividual)   // all individuals

        // is property an ObjectProperty?

        if (isOfType(p, OWL.ObjectProperty)) {
            domainP = allOfType(OWL.NamedIndividual)
            rangeP = allOfType(OWL.NamedIndividual)
        }
        /*
        else if (isOfType(p, datatypeProp)) {
            val (d, r) = computeDomainsDataProp(p)
            domainP = d
            rangeP = r
        }
        // is property type property?
        else */
        else if (p == RDF.type){
            // let's restrict ourselves to add type relations between individuals and classes
            domainP = Ind
            rangeP = allOfType(OWL.Class)
        }
        else if (p == RDFS.subClassOf){
            domainP = allOfType(OWL.Class)
            rangeP = allOfType(OWL.Class)
        }
        else if (p == RDFS.domain || p == RDFS.range){
            domainP = allOfType(OWL.ObjectProperty)
            rangeP = allOfType(OWL.Class)
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

                    else -> model.createResource("newPredicate:" + randomGenerator.nextInt())
                }
            }
            else
                getCandidates().random(randomGenerator)

        val p = model.getProperty(prop.toString())

        val (domainPunsorted, rangePunsorted) = computeDomainsProp(p)

        val domainP = domainPunsorted.toList().sortedBy { it.toString() }
        val rangeP = rangePunsorted.toList().sortedBy { it.toString() }


        // check, if there are candidates to add this relation
        if (domainP.any() && rangeP.any()) {
            var axiomCand = model.createStatement(domainP.random(randomGenerator), p, rangeP.random(randomGenerator))

            // test if axiom exists
            // try 10 times to find an axiom that does not exist, as this often works and it is fast
            var i = 0
            while (!validAddition(axiomCand) && i < 10) {
                // find new axiom
                axiomCand = model.createStatement(domainP.random(randomGenerator), p, rangeP.random(randomGenerator))
                i += 1
            }

            // test if a non-contained axiom was found
            val axiom =
                if (!validAddition(axiomCand))
                    // use expensive search for a valid axiom in case no valid one was found so far
                    costlySearchForValidAxiom(p, domainP.toSet(), rangeP.toSet())
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

// removes a statement, that uses the defined predicate
abstract class RemoveStatementByRelationMutation(model: Model) : RemoveStatementMutation(model) {
    abstract val targetPredicate : Resource

    override fun getCandidates(): List<Statement> {
        val l = model.listStatements().toList()
        val candidates  = mutableSetOf<Statement>()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.predicate == targetPredicate) {
                candidates.add(s)
            }
        }
        return filterMutableStatements(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        val c = config as SingleStatementConfiguration
        assert(c.getStatement().predicate == targetPredicate)
        super.setConfiguration(config)
    }
}

// removes a statement, that uses a predicate of the defined Type
abstract class RemoveStatementByTypeOfRelationMutation(model: Model) : RemoveStatementMutation(model) {
    abstract val targetType : Resource
    private val targetPredicates  get() = allOfType(targetType)

    override fun getCandidates(): List<Statement> {

        val l = model.listStatements().toList()
        val candidates  = mutableSetOf<Statement>()
        for (s in l) {
            // select statements that are not subClass relations
            if (targetPredicates.contains(s.predicate)) {
                candidates.add(s)
            }
        }
        return filterMutableStatements(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        val c = config as SingleStatementConfiguration
        assert(targetPredicates.contains(c.getStatement().predicate))
        super.setConfiguration(config)
    }
}

// similar to adding a relation, but all existing triples with this subject ond predicate are deleted
open class ChangeRelationMutation(model: Model) : AddRelationMutation(model) {

    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
       // return cand
        return filterMutableStatementsResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun computeDomainsProp(p: Property): Pair<Set<Resource>, Set<RDFNode>> {
        // use all individuals as domain that already have such an outgoing relation
        val domainP = model.listResourcesWithProperty(p).toSet()
        // use (restrictions of ) domain and range from super method
        val (domainRestricted, rangeP) = super.computeDomainsProp(p)
        return Pair(domainP.intersect(domainRestricted), rangeP)
    }

    override fun createMutation() {
        // create the mutation as usual (i.e. adding a new triple)
        super.createMutation()

        // find existing relations and remove them
        turnAdditionsToChanges()
    }
}

open class AddStatementMutation(model: Model) : Mutation(model) {
    override fun isApplicable(): Boolean {
        return true
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        super.setConfiguration(config)
    }

    override fun createMutation() {
        if (config != null) {
            val c = config as SingleStatementConfiguration
            val s = c.getStatement()
            addSet.add(s)
        }
        super.createMutation()
    }
}

open class RemoveNodeMutation(model: Model) : Mutation(model) {

    open fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates : MutableSet<Resource> = hashSetOf()
        for (s in l) {
            candidates.add(s.subject)
            if (s.`object`.isResource)
                candidates.add(s.`object`.asResource())
        }
        return filterMutableStatementsResource(candidates.toList()).toList().sortedBy { it.toString() }
    }

    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().any()
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        super.setConfiguration(config)
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
                getCandidates().randomOrNull(randomGenerator)

        if (res != null) {
            // select all axioms that contain the resource
            val l = model.listStatements().toList().toMutableList()
            for (s in l) {
                if (s.subject == res || s.predicate == res || s.`object` == res) {
                    removeSet.add(s)
                }
            }
        }

        super.createMutation()
    }
}

open class ReplaceNodeInStatementMutation(model: Model) : Mutation(model) {
    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().isNotEmpty()
    }

    // default: do not select any candidate
    open fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is DoubleStringAndStatementConfiguration)
        super.setConfiguration(config)
    }

    override fun createMutation() {
        if (!hasConfig)
            this.setConfiguration(getCandidates().random(randomGenerator))

        assert(config is DoubleStringAndStatementConfiguration)
        val c = config as DoubleStringAndStatementConfiguration


        val oldAxiom = c.getStatement()
        val newResource = model.createResource(c.getNewNode())
        val newProperty = model.createProperty(c.getNewNode())

        val newAxiom = when (c.getOldNode()) {
            oldAxiom.subject.toString() ->
                model.createStatement(newResource, oldAxiom.predicate, oldAxiom.`object`)
            oldAxiom.predicate.toString() ->
                model.createStatement(oldAxiom.subject, newProperty, oldAxiom.`object`)
            oldAxiom.`object`.toString() ->
                model.createStatement(oldAxiom.subject, oldAxiom.predicate, newResource)
            else ->
                oldAxiom    // if nothing matches: keep old axiom as nothing needs to be replaced
        }
        removeSet.add(oldAxiom)
        addSet.add(newAxiom)
        super.createMutation()
    }

    open fun createMutationDouble() {
        if (!hasConfig)
            this.setConfiguration(getCandidates().random(randomGenerator))

        assert(config is DoubleStringAndStatementConfiguration)
        val c = config as DoubleStringAndStatementConfiguration

        val oldAxiom = c.getStatement()
        val newResource = model.createTypedLiteral(c.getNewNode(), XSD.xdouble.toString())

        val newAxiom = when (c.getOldNode()) {
            oldAxiom.`object`.toString() ->
                model.createStatement(oldAxiom.subject, oldAxiom.predicate, newResource)
            else ->
                oldAxiom    // if nothing matches: keep old axiom as nothing needs to be replaced
        }
        removeSet.add(oldAxiom)
        addSet.add(newAxiom)
        super.createMutation()
    }
}

abstract class ReplaceNodeWithNode(model: Model) : Mutation(model) {

    // default values: anonymous resources
    var oldNode: Resource = model.createResource()
    var newNode: Resource = model.createResource()

    override fun createMutation() {
        for (s in model.listStatements()) {
            // try to replace nodes
            val newSubject = tryReplace(s.subject)
            val newPredicate = tryReplace(s.predicate.asResource())
            val newObject =
                if (s.`object`.isResource)
                    tryReplace(s.`object`.asResource())
                else
                    s.`object`

            // test, if something was replaced; if yes --> add to sets accordingly
            if (s.subject != newSubject || s.predicate.asResource() != newPredicate || s.`object` != newObject) {
                removeSet.add(s)

                addSet.add(model.createStatement(
                    newSubject,
                    model.createProperty(newPredicate.uri),
                    newObject
                ))
            }
        }
        super.createMutation()
    }

    // replaces the resource if possible
    // returns argument otherwise
    private fun tryReplace(r: Resource) : Resource {
        return if (r == oldNode) newNode else r
    }
}