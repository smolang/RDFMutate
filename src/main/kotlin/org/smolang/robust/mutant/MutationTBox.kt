package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.smolang.robust.randomGenerator

// adds the specified relation between two properties
abstract class AddRelationByTypesMutation(model: Model, verbose: Boolean) : AddStatementMutation(model, verbose) {
    abstract val addedRelation : Property
    abstract val sourceType : Resource  // type of potential sources
    abstract val targetType : Resource  // type of potential targets

    override fun createMutation() {
        val sources = allOfType(sourceType)
        val targets = allOfType(targetType)

        if (sources.isNotEmpty() && targets.isNotEmpty()) {
            val source = sources.random(randomGenerator)
            val target = targets.random(randomGenerator)

            val s = model.createStatement(
                source,
                addedRelation,
                target
            )
            val config = SingleStatementConfiguration(s)
            super.setConfiguration(config)
        }

        super.createMutation()
    }
}

// adds the specified relation between two classes
abstract class AddClassRelationMutation(model: Model, verbose: Boolean) : AddRelationByTypesMutation(model, verbose) {
    override val sourceType = owlClass
    override val targetType = owlClass
}

// adds relation between property and class
abstract class AddPropClassRelationMutation(model: Model, verbose: Boolean) : AddRelationByTypesMutation(model, verbose) {
    override val sourceType = objectProp
    override val targetType = owlClass
}



// adds the specified property (e.g. transitive) for property
abstract class AddTypeInformationMutation(model: Model, verbose: Boolean) : AddStatementMutation(model, verbose) {
    abstract val targetObjects : Resource   // types of objects to target, i.e. add the information to
    abstract val additionalType : Resource

    override fun createMutation() {
        val owlProperties = allOfType(targetObjects)

        if (owlProperties.isNotEmpty()) {
            val prop = owlProperties.random(randomGenerator)

            val s = model.createStatement(
                prop,
                rdfTypeProp,
                additionalType
            )
            val config = SingleStatementConfiguration(s)
            super.setConfiguration(config)
        }

        super.createMutation()
    }
}

//removes one (random) subclass axiom       // val m = Mutator
class RemoveSubclassRelationMutation(model: Model, verbose : Boolean) : RemoveStatementByRelationMutation(model, verbose) {
    override val targetPredicate = subClassProp
}

class AddSubclassRelationMutation(model: Model, verbose: Boolean) : AddClassRelationMutation(model, verbose) {
    override val addedRelation = subClassProp
}

class AddEquivalentClassRelationMutation(model: Model, verbose: Boolean) : AddClassRelationMutation(model, verbose) {
    override val addedRelation = equivClassProp
}

//removes one (random) equivClass axiom       // val m = Mutator
class RemoveEquivClassRelationMutation(model: Model, verbose : Boolean) : RemoveStatementByRelationMutation(model, verbose) {
    override val targetPredicate = equivClassProp
}

class AddDisjointClassRelationMutation(model: Model, verbose: Boolean) : AddClassRelationMutation(model, verbose) {
    override val addedRelation = disjointClassProp
}

class RemoveDisjointClassRelationMutation(model: Model, verbose : Boolean) : RemoveStatementByRelationMutation(model, verbose) {
    override val targetPredicate = disjointClassProp
}

class AddReflexiveObjectPropertyRelationMutation(model: Model, verbose: Boolean) : AddTypeInformationMutation(model, verbose) {
    override val additionalType = reflexiveProp
    override val targetObjects = objectProp
}

class AddTransitiveObjectPropertyRelationMutation(model: Model, verbose: Boolean) : AddTypeInformationMutation(model, verbose) {
    override val additionalType = transitiveProp
    override val targetObjects = objectProp
}

class AddObjectPropDomainMutation(model: Model, verbose: Boolean) : AddPropClassRelationMutation(model, verbose) {
    override val addedRelation = domainProp
}

class RemoveDomainRelationMutation(model: Model, verbose : Boolean) : RemoveStatementByRelationMutation(model, verbose) {
    override val targetPredicate = domainProp
}

class AddObjectPropRangeMutation(model: Model, verbose: Boolean) : AddPropClassRelationMutation(model, verbose) {
    override val addedRelation = rangeProp
}

class RemoveRangeRelationMutation(model: Model, verbose : Boolean) : RemoveStatementByRelationMutation(model, verbose) {
    override val targetPredicate = rangeProp
}



// removes one part of an "AND" in a logical axiom
class CEUAMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose)   {
    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:intersectionOf ?b. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for(r in res){
            val y = r.get("?y")
            val a = r.get("?a")
            val axiom = model.createStatement(a.asResource(),rdfFirst, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                y.toString(),
                owlThing.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        if (config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(config)
        else {
            assert(config is SingleStatementConfiguration)
            val con = config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                con.getStatement().`object`.toString(),
                owlThing.toString(),
                con.getStatement()
            )

            super.setConfiguration(c)
        }
    }
}

// removes one part of an "OR" in a logical axiom
class CEUOMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose)   {

    // selects names of nodes that should be removed / replaced by owl:Nothing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT * WHERE { " +
                "?x owl:unionOf ?b. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for(r in res){
            val y = r.get("?y")
            val a = r.get("?a")
            val axiom = model.createStatement(a.asResource(),rdfFirst, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                y.toString(),
                owlNothing.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        if (config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(config)
        else {
            assert(config is SingleStatementConfiguration)
            val con = config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                con.getStatement().`object`.toString(),
                owlNothing.toString(),
                con.getStatement()
            )

            super.setConfiguration(c)
        }
    }

}

// replace "AND" by "OR"
class ACATOMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose) {

    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "SELECT * WHERE { " +
                "?x owl:intersectionOf ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val x = r.get("?x")
            val y = r.get("?y")
            val axiom = model.createStatement(x.asResource(), intersectionProp, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                intersectionProp.toString(),
                unionProp.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        if (config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(config)
        else {
            assert(config is SingleStatementConfiguration)
            val con = config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                intersectionProp.toString(),
                unionProp.toString(),
                con.getStatement()
            )

            super.setConfiguration(c)
        }
    }
}


// replace "Or" by "And"
class ACOTAMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose) {
    // selects names of nodes that should be removed / replaced by owl:Thing
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "SELECT * WHERE { " +
                "?x owl:unionOf ?y. " +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val x = r.get("?x")
            val y = r.get("?y")
            val axiom = model.createStatement(x.asResource(), unionProp, y)
            //println("$a $y $axiom")
            ret += DoubleStringAndStatementConfiguration(
                unionProp.toString(),
                intersectionProp.toString(),
                axiom)
        }
        return ret.sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        if (config is DoubleStringAndStatementConfiguration)
            super.setConfiguration(config)
        else {
            assert(config is SingleStatementConfiguration)
            val con = config as SingleStatementConfiguration
            val c = DoubleStringAndStatementConfiguration(
                unionProp.toString(),
                intersectionProp.toString(),
                con.getStatement()
            )
            super.setConfiguration(c)
        }
    }

}


// replaces arguments in
class ToSiblingClassMutation(model: Model, verbose: Boolean): ReplaceNodeInStatementMutation(model, verbose) {

    // selects names of classes
    // TODO: also in other parts of logical axioms, e.g. after restrictions
    override fun getCandidates(): List<DoubleStringAndStatementConfiguration> {
        // withing union or intersection
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?a ?c ?cSibling WHERE { " +
                "?x (owl:unionOf | owl:intersectionOf) ?y. " +
                "?b (rdf:rest)* ?a." +
                "?a rdf:first ?c. " + // find class in union / intersection
                "?c rdf:type owl:Class." +
                "?c rdfs:subClassOf ?cSuper." +
                "?cSibling rdfs:subClassOf ?cSuper." +
                "}"

        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val ret = mutableListOf<DoubleStringAndStatementConfiguration>()
        for (r in res) {
            val a = r.get("?a")
            val c = r.get("?c")
            val cSibling = r.get("?cSibling")
            if (c.toString() != cSibling.toString()) { // check, if entities different
                val axiom = model.createStatement(a.asResource(), rdfFirst, c)
                //println("$c $cSibling $axiom")
                ret += DoubleStringAndStatementConfiguration(
                    c.toString(),
                    cSibling.toString(),
                    axiom)
            }
        }

        // after existential quantification
        val queryString2 = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "SELECT DISTINCT ?r ?c ?cSibling WHERE { " +
                "?r owl:someValuesFrom ?c. " + // find class after existential quantification
                "?r rdf:type owl:Restriction. " +
                "?c rdf:type owl:Class." +
                "?c rdfs:subClassOf ?cSuper. " +
                "?cSibling rdfs:subClassOf ?cSuper." +
                "}"
        val query2 = QueryFactory.create(queryString2)
        val res2 = QueryExecutionFactory.create(query2, model).execSelect()
        for (r in res2) {
            val restriction = r.get("?r")
            val c = r.get("?c")
            val cSibling = r.get("?cSibling")
            //println("$c $cSibling $restriction")
            if (c.toString() != cSibling.toString()) { // check, if entities different
                val axiom = model.createStatement(restriction.asResource(),someValuesFromProp, c)
                //println("$c $cSibling $axiom")
                ret += DoubleStringAndStatementConfiguration(
                    c.toString(),
                    cSibling.toString(),
                    axiom)
            }
        }

        return ret.sortedBy { it.toString() }
    }

}

class RemoveClassMutation(model: Model, verbose : Boolean) : RemoveNodeMutation(model, verbose) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // check, if statement is class declaration
            if (s.predicate == rdfTypeProp && s.`object` == owlClass) {
                candidates.add(s.subject)
            }
        }
        return filterMutatableAxiomsResource(candidates).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        // assert that the resource is really an individual
        val ind = (config as SingleResourceConfiguration).getResource()
        assert(isOfType(ind, owlClass))
        super.setConfiguration(config)
    }
}

class RemoveObjectPropertyMutation(model: Model, verbose : Boolean) : RemoveNodeMutation(model, verbose) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // check, if statement is object property declaration
            if (s.predicate == rdfTypeProp && s.`object` == objectProp) {
                candidates.add(s.subject)
            }
        }
        return filterMutatableAxiomsResource(candidates).sortedBy { it.toString() }
    }

    override fun setConfiguration(config: MutationConfiguration) {
        assert(config is SingleResourceConfiguration)
        // assert that the resource is really an individual
        val ind = (config as SingleResourceConfiguration).getResource()
        assert(isOfType(ind, owlClass))
        super.setConfiguration(config)
    }
}

class ReplaceClassWithTopMutation(model: Model, verbose: Boolean) : ReplaceNodeWithNode(model, verbose) {
    override fun createMutation() {
        // select a random class to be replaced
        // ignore classes that share their name with properties
        val classes = allOfType(owlClass).minus(allOfType(objectProp)).minus(allOfType(datatypeProp))

        // only replace, if at least one class is defined
        if (classes.any())
            oldNode = classes.random(randomGenerator)
        newNode = owlThing

        super.createMutation()
        // do not add statement that owl:Thing is an owl class
        addSet.remove(
            model.createStatement(owlThing, rdfTypeProp, owlClass)
        )
    }
}

class ReplaceClassWithBottomMutation(model: Model, verbose: Boolean) : ReplaceNodeWithNode(model, verbose) {
    override fun createMutation() {
        // select a random class to be replaced
        // ignore classes that share their name with properties
        val classes = allOfType(owlClass).minus(allOfType(objectProp)).minus(allOfType(datatypeProp))
        // only replace, if at least one class is defined
        if (classes.any())
            oldNode = classes.random(randomGenerator)

        newNode = owlNothing
        super.createMutation()

        // do not add statement that owl:Nothing is an owl class
        addSet.remove(
            model.createStatement(owlNothing, rdfTypeProp, owlClass)
        )
    }
}

class ReplaceClassWithSiblingMutation(model: Model, verbose: Boolean): ReplaceNodeWithNode(model, verbose) {
    override fun createMutation() {
        // select sibling classes
        // filter: old class should not also be property (safeguard because replacement might not be careful enough)
        val queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT ?s1 ?s2 WHERE { " +
                    "?s1 rdf:type owl:Class ." +
                    "?s2 rdf:type owl:Class ." +
                    "?parent rdf:type owl:Class ." +
                    "?s1 rdfs:subClassOf ?parent . " +
                    "?s2 rdfs:subClassOf ?parent ." +
                    "FILTER NOT EXISTS {" +
                        "?s1 rdf:type owl:ObjectProperty ." +
                    "}" +
                    "FILTER NOT EXISTS {" +
                        "?s1 rdf:type owl:DatatypeProperty ." +
                    "}" +
                "}"
        val query = QueryFactory.create(queryString)
        val res = QueryExecutionFactory.create(query, model).execSelect()
        val pairs = mutableListOf<Pair<Resource, Resource>>()
        for(r in res){
            val s1 = r.get("?s1")
            val s2 = r.get("?s2")
            if (s1 != s2)
                pairs.add(Pair(s1.asResource(), s2.asResource()))
        }
        // only do replacement if there is at least one candidate pair
        if (pairs.size > 0) {
            val pair = pairs.random(randomGenerator)
            oldNode = pair.first
            newNode = pair.second
        }
        super.createMutation()
    }
}