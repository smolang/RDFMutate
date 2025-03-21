package org.smolang.robust.mutant

import org.apache.jena.rdf.model.*
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.smolang.robust.randomGenerator
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.ComplexStatementBuilder

open class Mutation(var model: Model) {
    var hasConfig : Boolean = false
    open var config : MutationConfiguration? = null
    private var createdMutation : Boolean = false

    // set of axioms to add or delete in this mutation
    var addSet : MutableSet<Statement> = hashSetOf()
    var removeSet : MutableSet<Statement> = hashSetOf()

    // some objects to work with the inferred model
    private val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()

    // using this infModel assumes that "model" never changed
    // i.e. it is the inferred model at the time of initialisation
    val infModel: InfModel get() = ModelFactory.createInfModel(reasoner, model)

    // axioms that should be considered when selecting mutation
    private val mutatableAxioms: MutableSet<Statement> = hashSetOf()
    private fun considerMutatableAxioms() : Boolean {return mutatableAxioms.isNotEmpty()
    }

    val statementBuilder = ComplexStatementBuilder(model)


    // define some properties / resources that are use all the time
    val rdfTypeProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    val subClassProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf")
    val equivClassProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#equivalentClass")
    val disjointClassProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#disjointWith")

    val subPropertyProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
    val equivPropertyProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#equivalentProperty")
    val domainProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#domain")
    val rangeProp : Property = model.createProperty("http://www.w3.org/2000/01/rdf-schema#range")
    val funcProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#FunctionalProperty")
    val reflexiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#ReflexiveProperty")
    val irreflexiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#IrreflexiveProperty")
    val transitiveProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#TransitiveProperty")
    val oneOfProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#oneOf")

    val owlThing : Resource = model.createResource("http://www.w3.org/2002/07/owl#Thing")
    val owlNothing : Resource = model.createResource("http://www.w3.org/2002/07/owl#Nothing")

    val owlClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#Class")
    val namedInd : Resource = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual")
    val objectPropClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#ObjectProperty")
    val dataPropClass : Resource = model.createResource("http://www.w3.org/2002/07/owl#DatatypeProperty")

    val negPropAssertion : Resource = model.createResource("http://www.w3.org/2002/07/owl#NegativePropertyAssertion")
    val sourceIndProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#sourceIndividual")
    val assertionPropProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#assertionProperty")
    val targetIndProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#targetIndividual")
    val targetValue : Property = model.createProperty("http://www.w3.org/2002/07/owl#targetValue")

    val hasKey : Property = model.createProperty("http://www.w3.org/2002/07/owl#hasKey")

    val differentFromProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#differentFrom")
    val sameAsProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#sameAs")
    val propChainProp : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#propertyChainAxiom")

    val xsdBoolean : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#boolean")
    val xsdDecimal : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#decimal")
    val xsdDouble : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#double")
    val xsdString : Resource = model.createResource("http://www.w3.org/2001/XMLSchema#string")
    val rdfsLiteral : Resource = model.createResource("http://www.w3.org/2000/01/rdf-schema#Literal")
    val rdfPlainLiteral : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral")


    val datatypeClass : Resource = model.createResource("http://www.w3.org/2000/01/rdf-schema#Datatype")

    val intersectionProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#intersectionOf")
    val unionProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#unionOf")
    val someValuesFromProp : Property = model.createProperty("http://www.w3.org/2002/07/owl#someValuesFrom")

    val rdfListClass : Resource = model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
    val rdfFirst : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
    val rdfRest : Property = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
    val rdfNil : Resource = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil")

    private val languageTags = listOf("en", "zh", "hi", "es",  "fr", "de")

    // some example data values, used when adding data relation; all in EL profile
    val exampleElDataValues get() =
        run {
            val num1 = "${randomGenerator.nextInt(0,100)}.${randomGenerator.nextInt(0,100)}"
            val num2 = "-${randomGenerator.nextInt(0,100)}.${randomGenerator.nextInt(0,100)}"
            val literal = "someText${randomGenerator.nextInt(0,10)}"
            setOf(
                // decimals
                model.createTypedLiteral(num1, xsdDecimal.toString()),
                model.createTypedLiteral(num2, xsdDecimal.toString()),
                // literals (might have language tag)
                model.createLiteral(literal),
                model.createLiteral(literal, languageTags.random(randomGenerator)),
                // strings
                model.createTypedLiteral(literal, xsdString.toString())
            )
        }
    val allElDataTypes get() =
        listOf(
            rdfPlainLiteral,
            model.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral"),
            rdfsLiteral,
            model.createResource("http://www.w3.org/2002/07/owl#real"),
            model.createResource("http://www.w3.org/2002/07/owl#rational"),
            xsdDecimal,
            model.createResource("http://www.w3.org/2001/XMLSchema#integer"),
            model.createResource("http://www.w3.org/2001/XMLSchema#nonNegativeInteger"),
            xsdString,
            model.createResource("http://www.w3.org/2001/XMLSchema#normalizedString"),
            model.createResource("http://www.w3.org/2001/XMLSchema#token"),
            model.createResource("http://www.w3.org/2001/XMLSchema#Name"),
            model.createResource("http://www.w3.org/2001/XMLSchema#NCName"),
            model.createResource("http://www.w3.org/2001/XMLSchema#NMTOKEN"),
            model.createResource("http://www.w3.org/2001/XMLSchema#hexBinary"),
            model.createResource("http://www.w3.org/2001/XMLSchema#base64Binary"),
            model.createResource("http://www.w3.org/2001/XMLSchema#anyURI"),
            model.createResource("http://www.w3.org/2001/XMLSchema#dateTime"),
            model.createResource("http://www.w3.org/2001/XMLSchema#dateTimeStamp"),
            )


    // empty Axiom
    private val emptyProp : Property = model.createProperty("emptyAxiomProp")
    val emptyAxiom : Statement = model.createStatement(model.createResource(), emptyProp, model.createResource())

    // constructor that creates mutation with configuration
    constructor(model: Model, config: MutationConfiguration) : this(model) {
        this.setConfiguration(config)
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

    open fun setConfiguration(config : MutationConfiguration) {
        hasConfig = true
        this.config = config
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

        mainLogger.info("applying mutation ${toString()}")
        mainLogger.info("removing: axioms $removeSet")
        mainLogger.info("adding: axioms $addSet")


        // copy all statements that are not deleteSet
        model.listStatements().forEach {
            if (!removeSet.contains(it)) m.add(it)}

        addSet.forEach {
            m.add(it)
        }
        // register prefixes from old model into new one
        m.setNsPrefixes(model.nsPrefixMap)
        return m
    }


    fun allNodes() : Set<Resource> {
        val l = model.listStatements()
        val modes : MutableSet<Resource> = hashSetOf()
        for (s in l) {
            // select statements that are not subClass relations
            modes.add(s.subject)
            if (s.`object`.isResource)
                modes.add(s.`object`.asResource())
        }
        return modes.toSet()
    }

    fun isOfType(i : Resource, t : Resource) : Boolean {
        return model.listStatements(i, rdfTypeProp, t).hasNext()
    }

    fun allOfType(t : Resource) : Set<Resource> {
        return model.listResourcesWithProperty(rdfTypeProp, t).toSet()
    }

    fun isOfInferredType(i : Resource, t : Resource) : Boolean {
        return infModel.listStatements(i, rdfTypeProp, t).hasNext()
    }

    fun allOfInferredType(t : Resource) : Set<Resource> {
        return infModel.listResourcesWithProperty(rdfTypeProp, t).toSet()
    }

    // iterate through the axioms to add and remove existing relations with same subject and predicate
    fun turnAdditionsToChanges() {
        // find existing relations and remove them
        for (axiom in addSet) {
            for (existingAxiom in model.listStatements(
                axiom.subject, axiom.predicate, null as RDFNode?
            ))
                removeSet.add(existingAxiom)
        }
    }

    // returns all elements from the linked a list in the ontology that starts in "head"
    fun allElementsInList(head: Resource) : List<RDFNode> {
        // assure that element is really a list
        if (!model.contains(model.createStatement(
            head,
            rdfTypeProp,
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
            if (rest.contains(rdfNil) || rest.isEmpty())
                // base case + if rest is empty --> error in schema as end is not correctly marked
                return list
            else {
                // recursive call
                val restElements = allElementsInList(rest.single().asResource())
                list.addAll(restElements)
                return list
            }
        }
    }

    override fun toString() : String {
        val className = this.javaClass.toString().removePrefix("class mutant.").removePrefix("class org.smolang.robust.mutant.")
        if (!hasConfig)
            return "$className(random)"
        else {
            val config = config.toString()
            return "$className(config=${config})"
        }
    }

    // checks, if anything in the statement starts with the provided prefix
    private  fun hasPrefix (stat: Statement, prefix : String) : Boolean {
        if (hasPrefix(stat.subject, prefix))
            return true
        if (hasPrefix(stat.predicate, prefix))
            return true
        if (stat.`object`.isResource && hasPrefix(stat.`object`.asResource(), prefix))
            return true

        return false
    }

    private  fun containsResource (stat: Statement, res: Resource) : Boolean {
        if (stat.subject.asResource() == res)
            return true
        if (stat.predicate.asResource() == res)
            return true
        if (stat.`object`.isResource && stat.`object`.asResource() == res)
            return true

        return false
    }

    private  fun hasPrefix (r: Resource, prefix : String) : Boolean {
        return r.toString().startsWith(prefix)
    }

    fun addMutatableAxiom(s: Statement) {
        mutatableAxioms.add(s)
    }

    fun filterMutatableAxioms(l: List<Statement>): List<Statement> {
        val lFiltered: MutableList<Statement> = mutableListOf()
        for (s in l) {
            if (!considerMutatableAxioms())
                lFiltered.add(s)
            else if (mutatableAxioms.contains(s))
                    lFiltered.add(s)
        }
        return lFiltered
    }

    fun filterMutatableAxiomsResource(l: List<Resource>): List<Resource> {
        val lFiltered: MutableList<Resource> = mutableListOf()
        for (r in l) {
            if (!considerMutatableAxioms())
                lFiltered.add(r)
            else for (p in mutatableAxioms)
                if (containsResource(p, r))
                    lFiltered.add(r)
        }
        return lFiltered
    }

}

open class RemoveStatementMutation(model: Model) : Mutation(model) {

    open fun getCandidates(): List<Statement> {
        val list = model.listStatements().toList()
        val f = filterMutatableAxioms(list)
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

        if (isOfType(p, irreflexiveProp)) {
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
        if (isOfInferredType(p, funcProp)) {
            // delete outgoing relations, so that relation remains functional
            // i.e. we are quite restrictive here, this makes only sense in the case of a unique name assumption
            // do this for all superProps of the property
            for (superPropAxiom in infModel.listStatements(p, subPropertyProp, null as RDFNode?)) {
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

        assert(isOfType(p, objectPropClass))
        // compute domains

        val Ind = allOfType(namedInd)   // all individuals

        val domains = infModel.listStatements(p, domainProp, null as RDFNode?).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        // compute range
        val ranges = infModel.listStatements(p, rangeProp, null as RDFNode?).toSet()
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

        assert(isOfType(p, dataPropClass))

        // compute domains

        val Ind = allOfType(namedInd)   // all individuals

        val domains = infModel.listStatements(p, domainProp, null as RDFNode?).toSet()
        var domainP : MutableSet<Resource> = Ind.toMutableSet()
        domains.forEach {
            domainP = domainP.intersect(allOfInferredType(it.`object`.asResource())).toMutableSet()
        }

        val ranges = model.listObjectsOfProperty(p, rangeProp).toSet()

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
                    r.add(xsdBoolean)
                    r.add(xsdDouble)
                    r.add(xsdDecimal)
                    r.random(randomGenerator)
                }

            // check different classes of data properties, for which we can determine the domain
            if (range == xsdBoolean) {
                val trueNode = model.createTypedLiteral("true", xsdBoolean.toString())
                val falseNode = model.createTypedLiteral("false", xsdBoolean.toString())
                rangeP = hashSetOf(trueNode, falseNode)
            }
            else if (range == xsdDecimal) {
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
                rangeP = hashSetOf(model.createTypedLiteral(data, xsdDecimal.toString()))

            }
            else if (range == xsdDouble) {
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
                rangeP = hashSetOf(model.createTypedLiteral(data, xsdDouble.toString()))

            }
            else if (isOfType(range.asResource(), datatypeClass)) {
                // check if it is a list of statements
                val oneOf = model.listObjectsOfProperty(
                    range.asResource(),
                    oneOfProp
                ).toSet()
                if (oneOf.size != 1){
                    mainLogger.warn("can not add data relation. Property ${p.localName} is marked as 'Datatype' but does" +
                                " not provide one 'oneOf'-relation ")
                }
                else {
                    val list = oneOf.single()
                    if (!isOfType(list.asResource(), rdfListClass)){
                        mainLogger.warn("can not add data relation. Property ${p.localName} has not a list as 'oneOf'.")
                    }
                    else {
                        // collect elements of list
                        rangeP = allElementsInList(list.asResource()).toMutableSet()
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

        val Ind = allOfType(namedInd)   // all individuals

        // is property an ObjectProperty?

        if (isOfType(p, objectPropClass)) {
            domainP = allOfType(namedInd)
            rangeP = allOfType(namedInd)
        }
        /*
        else if (isOfType(p, datatypeProp)) {
            val (d, r) = computeDomainsDataProp(p)
            domainP = d
            rangeP = r
        }
        // is property type property?
        else */
        else if (p == rdfTypeProp){
            // let's restrict ourselves to add type relations between individuals and classes
            domainP = Ind
            rangeP = allOfType(owlClass)
        }
        else if (p == subClassProp){
            domainP = allOfType(owlClass)
            rangeP = allOfType(owlClass)
        }
        else if (p == domainProp || p == rangeProp){
            domainP = allOfType(objectPropClass)
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
        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
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
        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
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
        return filterMutatableAxiomsResource(cand.toList()).sortedBy { it.toString() }
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
        return filterMutatableAxiomsResource(candidates.toList()).toList().sortedBy { it.toString() }
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
        val newResource = model.createTypedLiteral(c.getNewNode(), xsdDouble.toString())

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




