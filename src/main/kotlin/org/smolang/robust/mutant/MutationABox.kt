package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.smolang.robust.randomGenerator
import org.smolang.robust.mainLogger

class AddInstanceMutation(model: Model) : Mutation(model) {
    // returns all classes
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
        return ret.sorted()
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesString(ret.toList()).sorted()
    }

    override fun isApplicable(): Boolean {
        return hasConfig || getCandidates().isNotEmpty()
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        super.setConfiguration(config)
    }

    override fun createMutation() {
        val m = ModelFactory.createDefaultModel()
        val owlClass =
            if (hasConfig){
                assert(config is SingleResourceConfiguration)
                val c = config as SingleResourceConfiguration
                c.getResource().toString()
            }
            else {
                val cand = getCandidates()

                if (cand.isNotEmpty())
                    cand.random(randomGenerator)
                else    // create class name if there is no class in KG
                    "arbitraryClass:" + randomGenerator.nextInt(0,Int.MAX_VALUE)
            }


        val instanceName =
            if (hasConfig && config is StringAndResourceConfiguration) {
                val c = config as StringAndResourceConfiguration
                c.getString()
            }
            else
                "inner:asd"+ randomGenerator.nextInt(0,Int.MAX_VALUE)
        // create new "type" relation for the individual and the selected class
        val s = m.createStatement(
            m.createResource(instanceName),
            m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            m.createResource(owlClass))

        addSet.add(s)
        super.createMutation()
    }
}

open class RemoveObjectPropertyRelationMutation(model: Model) : RemoveStatementMutation(model) {
    override fun getCandidates(): List<Statement> {
        val allProps = allOfType(objectPropClass)
        val candidates : MutableSet<Statement> = hashSetOf()
        allProps.forEach {
            candidates.addAll(model.listStatements(
                null as Resource?, model.getProperty(it.toString()), null as RDFNode?
            ).toSet())
        }
        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        when (config) {
            is SingleResourceConfiguration -> {
                // select a random element to delete
                val p = model.getProperty(config.getResource().toString())
                val cand = model.listStatements(null as Resource?, p, null as RDFNode?).toSet()

                val axiom =
                    if (cand.any())
                        cand.random(randomGenerator)
                    else {
                        mainLogger.info("no relation with this property exists / can be deleted.")
                        emptyAxiom
                    }
                super.setConfiguration(
                    SingleStatementConfiguration(axiom)
                )
            }
            is SingleStatementConfiguration -> {
                super.setConfiguration(config)
            }
        }
    }
}

open class AddObjectPropertyRelationMutation(model: Model) : AddRelationMutation(model) {
    override fun getCandidates() : List<Resource> {
        val cand = allOfType(objectPropClass)
        return cand.sortedBy { it.toString() }
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun computeDomainsProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        val domain = allOfType(namedInd)
        val range = allOfType(namedInd)
        return Pair(domain, range)
    }

    override fun setConfiguration(config: MutationConfiguration) {
        val p =
            when (config) {
                is SingleResourceConfiguration -> {
                    config.getResource()
                }

                is SingleStatementConfiguration -> {
                    config.getStatement().predicate
                }

                else -> model.createResource("newObjectProp" + randomGenerator.nextInt())
            }

        // check that p is really an ObjectProperty, if it exists in the model at all
        if (model.listResourcesWithProperty(null ).toSet().contains(p.asResource()))
            if (!isOfType(p.asResource(), objectPropClass)) {
                mainLogger.warn("Resource $p is not an object property but is used as such in configuration.")
            }

        super.setConfiguration(config)
    }
}

abstract class AddNegativePropertyRelationMutation(model: Model) : Mutation(model) {
    abstract val typeOfProperty :Resource
    abstract val domain : Set<Resource>
    abstract val range : Set<RDFNode>
    abstract val relationToTarget : Property    // relation to target node

    val properties : Set<Resource>
        get() =allOfType(typeOfProperty)

    override fun isApplicable(): Boolean {
        return (properties.isNotEmpty() && domain.isNotEmpty() && range.isNotEmpty())
    }

    override fun createMutation() {
        if (isApplicable()) {
            val source = domain.random(randomGenerator)
            val target = range.random(randomGenerator)
            val property = properties.random(randomGenerator)

            val axiomNode = model.createResource()

            val typeAxiom = model.createStatement(axiomNode, rdfTypeProp, negPropAssertion)
            val sourceAxiom = model.createStatement(axiomNode, sourceIndProp, source)
            val propAxiom = model.createStatement(axiomNode, assertionPropProp, property)
            val targetAxiom = model.createStatement(axiomNode, relationToTarget, target)

            addSet.add(typeAxiom)
            addSet.add(sourceAxiom)
            addSet.add(propAxiom)
            addSet.add(targetAxiom)

            super.createMutation()
        }
    }
}

class AddNegativeObjectPropertyRelationMutation(model: Model) : AddNegativePropertyRelationMutation(model) {
    override val typeOfProperty = objectPropClass
    override val domain = allOfType(namedInd)
    override val range = allOfType(namedInd)
    override val relationToTarget = targetIndProp
}

class RemoveNegativePropertyAssertionMutation(model: Model) : RemoveNodeMutation(model) {
    override fun getCandidates(): List<Resource> {
        return allOfType(negPropAssertion).toList()
    }
}

open class ChangeObjectPropertyRelationMutation(model: Model) : AddObjectPropertyRelationMutation(model) {
    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
        return cand
        //return filterMutatableAxiomsResource(cand.toList()).sortedBy { it.toString() }
    }

    override fun createMutation() {
        // create the mutation as usual (i.e. adding a new triple)
        super.createMutation()

        // find existing relations and remove them
        turnAdditionsToChanges()
    }
}

// adds data property relation
// does not care about declared ranges or domains of relation
class BasicAddDataPropertyRelationMutation(model: Model) : AddRelationMutation(model) {
    override fun getCandidates() : List<Resource> {
        val cand = allOfType(dataPropClass)
        return cand.sortedBy { it.toString() }
    }

    override fun computeDomainsProp(p : Property) : Pair<Set<Resource>, Set<RDFNode>> {
        val domain = allOfType(namedInd)
        // different options for values
        val range = exampleElDataValues

        return Pair(domain, range)
    }

    override fun setConfiguration(config: MutationConfiguration) {
        val p =
            when (config) {
                is SingleResourceConfiguration -> {
                    config.getResource()
                }

                is SingleStatementConfiguration -> {
                    config.getStatement().predicate
                }

                else -> model.createResource("newObjectProp" + randomGenerator.nextInt())
            }

        // check that p is really an ObjectProperty, if it exists in the model at all
        if (model.listResourcesWithProperty(null ).toSet().contains(p.asResource()))
            if (!isOfType(p.asResource(), dataPropClass)) {
                mainLogger.warn("Resource $p is not a data property but is used as such in configuration.")
            }

        super.setConfiguration(config)
    }
}

class RemoveDataPropertyRelationMutation(model: Model) : RemoveStatementByTypeOfRelationMutation(model) {
    override val targetType = dataPropClass
}

class AddNegativeDataPropertyRelationMutation(model: Model) : AddNegativePropertyRelationMutation(model) {
    override val typeOfProperty = dataPropClass
    override val domain = allOfType(namedInd)
    override val range = exampleElDataValues
    override val relationToTarget = targetValue
}

// adds new individual to the ontology
class AddIndividualMutation(model: Model) : AddStatementMutation(model) {
    init {
        val individualName = "newIndividual:number"+ randomGenerator.nextInt(0,Int.MAX_VALUE)
        // create new "type" relation for the individual and the selected class
        val s = model.createStatement(
            model.createResource(individualName),
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            namedInd)
        val config = SingleStatementConfiguration(s)
        super.setConfiguration(config)
    }
}

class RemoveIndividualMutation(model: Model) : RemoveNodeMutation(model) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // select statements that are not subClass relations
            if (s.predicate == rdfTypeProp && s.`object` == namedInd) {
                candidates.add(s.subject)
            }
        }
        return filterMutatableAxiomsResource(candidates).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        // assert that the resource is really an individual
        val ind = (config as SingleResourceConfiguration).getResource()
        assert(isOfType(ind, namedInd))
        super.setConfiguration(config)
    }
}

class ChangeDataPropertyMutation(model: Model) : ChangeRelationMutation(model) {
    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
        // only select data properties
        val newCand =  cand.filter { isOfInferredType(it, dataPropClass)}
        return newCand
    }
}

class AddClassAssertionMutation (model: Model) : AddStatementMutation(model) {
    init {
        // collect all OWL classes and individuals
        val classes = allOfType(owlClass)
        val individuals = allOfType(namedInd)

        val s =
            if (individuals.isEmpty() || classes.isEmpty())
                emptyAxiom
            else {
                model.createStatement(
                    individuals.random(randomGenerator),
                    rdfTypeProp,
                    classes.random(randomGenerator)
                )
            }

        super.setConfiguration(SingleStatementConfiguration(s))

    }
}

class RemoveClassAssertionMutation(model: Model) : RemoveStatementMutation(model) {
    override fun getCandidates(): List<Statement> {
        val candidates : MutableList<Statement> = mutableListOf()

        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x rdf:type ?C. " +
                "?C rdf:type owl:Class ." +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        for(r in res){
            val x = r.get("?x")
            val C = r.get("?C")
            val statement = model.createStatement(x.asResource(),rdfTypeProp, C)
            candidates.add(statement)
        }

        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        val c = config as SingleStatementConfiguration
        assert(c.getStatement().predicate == rdfTypeProp)
        super.setConfiguration(config)
    }
}

open class ChangeDoubleMutation(model: Model): ReplaceNodeInStatementMutation(model) {
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n " +
                "SELECT DISTINCT * WHERE { " +
                "?x ?p ?d. " +
                "FILTER(DATATYPE(?d) = xsd:double)." +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val x = r.get("?x")
            val p = r.get("?p")
            val d = r.get("?d")
            val prop = model.getProperty(p.toString())
            val axiom = model.createStatement(x.asResource(), prop, d)

            // compute new value by multiplying old one

            // factor: values around 1 more likely than larger factors
            var factor = (1.0/ randomGenerator.nextDouble(0.0,2.0) + 0.5)    // factor between 1...inf
            if(randomGenerator.nextBoolean()) // negative factor
                factor = -factor
            if (randomGenerator.nextBoolean()) // inverse
                factor = 1.0/factor
            val newDouble = d.toString()
                .removeSuffix("^^http://www.w3.org/2001/XMLSchema#double")
                .removeSuffix("^^xsd:double")
                .removeSuffix("\"")
                .removePrefix("\"")
                .toDouble() * factor
            ret += DoubleStringAndStatementConfiguration(
                d.toString(),
                newDouble.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun createMutation() {
        super.createMutationDouble()
    }
}

// adds the specified relation between two individuals
abstract class AddIndividualRelationMutation(model: Model) : AddStatementMutation(model) {
    abstract val targetRelation : Property

    override fun createMutation() {
        val individuals = allOfType(namedInd)

        if (individuals.isNotEmpty()) {
            val ind1 = individuals.random(randomGenerator)
            val ind2 = individuals.random(randomGenerator)

            val s = model.createStatement(
                ind1,
                targetRelation,
                ind2
            )
            val config = SingleStatementConfiguration(s)
            super.setConfiguration(config)
        }

        super.createMutation()
    }
}

class AddSameIndividualAssertionMutation(model: Model) : AddIndividualRelationMutation(model) {
    override val targetRelation = sameAsProp
}

class RemoveSameIndividualAssertionMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = sameAsProp
}

class AddDifferentIndividualAssertionMutation(model: Model) : AddIndividualRelationMutation(model) {
    override val targetRelation = differentFromProp
}

class RemoveDifferentIndividualAssertionMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = differentFromProp
}

