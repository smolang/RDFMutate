package org.smolang.robust.mutant

import org.apache.jena.rdf.model.*
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.update.UpdateAction
import org.apache.jena.update.UpdateRequest
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.randomGenerator
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.ComplexStatementBuilder

open class Mutation(val model: Model) {
    var hasConfig : Boolean = false
    open var config : MutationConfiguration? = null
    private var createdMutation : Boolean = false

    // set of axioms to add or delete in this mutation
    var addSet : MutableSet<Statement> = hashSetOf()
    var removeSet : MutableSet<Statement> = hashSetOf()
    var updateRequestList : MutableList<UpdateRequest> = mutableListOf()    // order is important for updates

    // some objects to work with the inferred model
    private val reasoner: Reasoner = ReasonerRegistry.getOWLReasoner()

    // using this infModel assumes that "model" never changed
    // i.e. it is the inferred model at the time of initialisation
    val infModel: InfModel get() = ModelFactory.createInfModel(reasoner, model)

    // axioms that should be considered when selecting mutation
    private val mutableStatements: MutableSet<Statement> = hashSetOf()
    private fun considerMutableAxioms() : Boolean {return mutableStatements.isNotEmpty()
    }

    val statementBuilder = ComplexStatementBuilder(model)

    private val languageTags = listOf("en", "zh", "hi", "es",  "fr", "de")

    // some example data values, used when adding data relation; all in EL profile
    val exampleElDataValues get() =
        run {
            val num1 = "${randomGenerator.nextInt(0,100)}.${randomGenerator.nextInt(0,100)}"
            val num2 = "-${randomGenerator.nextInt(0,100)}.${randomGenerator.nextInt(0,100)}"
            val literal = "someText${randomGenerator.nextInt(0,10)}"
            setOf(
                // decimals
                model.createTypedLiteral(num1, XSD.decimal.toString()),
                model.createTypedLiteral(num2, XSD.decimal.toString()),
                // literals (might have language tag)
                model.createLiteral(literal),
                model.createLiteral(literal, languageTags.random(randomGenerator)),
                // strings
                model.createTypedLiteral(literal, XSD.xstring.toString())
            )
        }
    val allElDataTypes get() =
        listOf(
            RDF.PlainLiteral,
            RDF.xmlLiteral,
            RDFS.Literal,
            OWL.real,
            OWL.rational,
            XSD.decimal,
            XSD.integer,
            XSD.nonNegativeInteger,
            XSD.xstring,
            XSD.normalizedString,
            XSD.token,
            XSD.Name,
            XSD.NCName,
            XSD.NMTOKEN,
            XSD.hexBinary,
            XSD.base64Binary,
            XSD.anyURI,
            XSD.dateTime,
            XSD.dateTimeStamp
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
        m.updateRequestList.forEach { updateRequestList.add(it) }
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
        // register prefixes from old model into new one
        m.setNsPrefixes(model.nsPrefixMap)

        // user needs to use either update list OR add- and remove-set
        if (updateRequestList.isNotEmpty() &&
            (addSet.isNotEmpty() || removeSet.isNotEmpty())) {
            mainLogger.error("Mutation has both, updateRequests and elements in addSet / removeSet. " +
                    "This is not allowed. No mutation is performed")
            // copy all statements
            model.listStatements().forEach { m.add(it)}
            return m
        }

        mainLogger.info("applying mutation ${toString()}")


        // use update list for mutation
        if (updateRequestList.isNotEmpty()) {
            // copy all statements
            model.listStatements().forEach { m.add(it)}


            // apply updates
            updateRequestList.forEach { update ->
                mainLogger.info("use update: update $update")
                try {
                    UpdateAction.execute(update, m)
                } catch (e: Exception) {
                    mainLogger.error("Executing update failed. Raised exception: $e")
                    mainLogger.warn("This update is ignored.")
                }
            }
            return m
        }

        // use add and delete set for mutation

        // clean the sets from empty axioms
        for (axiom in addSet) {
            if (axiom.predicate == emptyProp)
                addSet.remove(axiom)
        }
        for (axiom in removeSet) {
            if (axiom.predicate == emptyProp)
                removeSet.remove(axiom)
        }

        mainLogger.info("removing: axioms $removeSet")
        mainLogger.info("adding: axioms $addSet")

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
        return model.listStatements(i, RDF.type, t).hasNext()
    }

    fun allOfType(t : Resource) : Set<Resource> {
        return model.listResourcesWithProperty(RDF.type, t).toSet()
    }

    fun isOfInferredType(i : Resource, t : Resource) : Boolean {
        return infModel.listStatements(i, RDF.type, t).hasNext()
    }

    fun allOfInferredType(t : Resource) : Set<Resource> {
        return infModel.listResourcesWithProperty(RDF.type, t).toSet()
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

    fun addMutableStatements(s: Statement) {
        mutableStatements.add(s)
    }

    fun filterMutableStatements(l: List<Statement>): List<Statement> {
        val lFiltered: MutableList<Statement> = mutableListOf()
        for (s in l) {
            if (!considerMutableAxioms())
                lFiltered.add(s)
            else if (mutableStatements.contains(s))
                    lFiltered.add(s)
        }
        return lFiltered
    }

    fun filterMutableStatementsResource(l: List<Resource>): List<Resource> {
        val lFiltered: MutableList<Resource> = mutableListOf()
        for (r in l) {
            if (!considerMutableAxioms())
                lFiltered.add(r)
            else for (p in mutableStatements)
                if (containsResource(p, r))
                    lFiltered.add(r)
        }
        return lFiltered
    }

}



