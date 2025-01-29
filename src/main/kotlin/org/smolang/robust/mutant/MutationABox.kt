package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.*
import org.smolang.robust.randomGenerator


class AddInstanceMutation(model: Model, verbose : Boolean) : Mutation(model, verbose) {
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
            else
                getCandidates().random(randomGenerator)
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

open class RemoveObjectPropertyRelationMutation(model: Model, verbose : Boolean) : RemoveStatementMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val allProps = allOfType(objectProp)
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
                        if (verbose)
                            println("no relation with this property exists / can be deleted")
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

open class AddObjectPropertyRelationMutation(model: Model, verbose: Boolean) : AddRelationMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand = ArrayList<Resource>()
        val l = model.listStatements().toList()
        for (s in l) {
            if (s.predicate == typeProp && s.`object` == objectProp)
                cand.add(s.subject)
        }
        return cand.sortedBy { it.toString() }
        // we do not filter, when we add stuff, only when we remove
        //return filterRelevantPrefixesResource(cand.toList()).sortedBy { it.toString() }
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

                else -> model.createResource()
            }

        // check that p is really an ObjectProperty, if it exists in the model at all
        if (model.listResourcesWithProperty(null ).toSet().contains(p.asResource()))
            if (!isOfType(p.asResource(), objectProp)) {
                println("WARNING: resource $p is not an object property but is used as such in configuration.")
            }

        super.setConfiguration(config)
    }
}

open class ChangeObjectPropertyRelationMutation(model: Model, verbose: Boolean) : AddObjectPropertyRelationMutation(model, verbose) {
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

// adds new individual to the ontology
class AddIndividualMutation(model: Model, verbose: Boolean) : AddStatementMutation(model, verbose) {
    init {
        val individualName = "newIndividual:number"+ randomGenerator.nextInt(0,Int.MAX_VALUE)
        // create new "type" relation for the individual and the selected class
        val s = model.createStatement(
            model.createResource(individualName),
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#","type"),
            namedInd)
        val config = SingleStatementConfiguration(s);
        super.setConfiguration(config)
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

class ChangeDataPropertyMutation(model: Model, verbose: Boolean) : ChangeRelationMutation(model, verbose) {
    override fun getCandidates() : List<Resource> {
        val cand =  super.getCandidates()
        // only select data properties
        val newCand =  cand.filter { isOfInferredType(it, dataProp)}
        return newCand
    }
}

class AddClassAssertionMutation (model: Model, verbose: Boolean) : AddStatementMutation(model, verbose) {
    init {
        // collect all OWL classes and individuals
        val classes = allOfType(owlClass)
        val individuals = allOfType(namedInd)

        val s = model.createStatement(
            individuals.random(randomGenerator),
            typeProp,
            classes.random(randomGenerator)
        )

        super.setConfiguration(SingleStatementConfiguration(s))

    }
}

class RemoveClassAssertionMutation(model: Model, verbose: Boolean) : RemoveStatementMutation(model, verbose) {
    override fun getCandidates(): List<Statement> {
        val candidates : MutableList<Statement> = mutableListOf();

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
            val statement = model.createStatement(x.asResource(),typeProp, C)
            candidates.add(statement)
        }

        return filterMutatableAxioms(candidates.toList()).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleStatementConfiguration)
        val c = config as SingleStatementConfiguration
        assert(c.getStatement().predicate == typeProp)
        super.setConfiguration(config)
    }
}

open class ChangeDoubleMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose) {
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
            //println("$x $prop $d")

            // compute new value by multiplying old one

            // factor: values around 1 more likely than larger factors
            var factor = (1.0/ randomGenerator.nextDouble(0.0,2.0) + 0.5)    // factor between 1...inf
            if(randomGenerator.nextBoolean()) // negative factor
                factor = -factor
            if (randomGenerator.nextBoolean()) // inverse
                factor = 1.0/factor
            val newDouble = d.toString().removeSuffix("^^http://www.w3.org/2001/XMLSchema#double").toDouble() * factor
            //println(newDouble)
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