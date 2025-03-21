package org.smolang.robust.mutant

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.ComplexStatementBuilder

// adds the specified relation between two properties
abstract class AddRelationByTypesMutation(model: Model) : AddStatementMutation(model) {
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
abstract class AddClassRelationMutation(model: Model) : AddRelationByTypesMutation(model) {
    override val sourceType = owlClass
    override val targetType = owlClass
}

// adds relation between object property and class
abstract class AddObjectPropClassRelationMutation(model: Model) : AddRelationByTypesMutation(model) {
    override val sourceType = objectPropClass
    override val targetType = owlClass
}

// adds relation between data property and class
abstract class AddDataPropClassRelationMutation(model: Model) : AddRelationByTypesMutation(model) {
    override val sourceType = dataPropClass
    override val targetType = owlClass
}

// adds relation between two properties
abstract class AddRelationBetweenObjectPropMutation(model: Model) : AddRelationByTypesMutation(model) {
    override val sourceType = objectPropClass
    override val targetType = objectPropClass
}

// adds relation between two properties
abstract class AddRelationBetweenDataPropMutation(model: Model) : AddRelationByTypesMutation(model) {
    override val sourceType = dataPropClass
    override val targetType = dataPropClass
}


// adds the specified property (e.g. transitive) for property
abstract class AddTypeInformationMutation(model: Model) : AddStatementMutation(model) {
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

// adds an objet with the specified type
abstract class DeclareObjectOfTypeMutation(model: Model) : AddStatementMutation(model) {
    abstract val targetType : Resource
    open val prefix = "newObject"

    override fun createMutation() {
        val newClass = model.createResource("$prefix:"+ randomGenerator.nextInt())
        val config = SingleStatementConfiguration(
            model.createStatement(
                newClass,
                rdfTypeProp,
                targetType
            )
        )
        setConfiguration(config)
        super.createMutation()
    }
}

// add as subclass axiom where one side of axiom is a complex class expression
abstract class AddComplexSubClassAxiomMutation(model: Model) : Mutation(model) {
    // returns statements representing the complex class expression
    abstract fun getComplexClassExpression(head : Resource) : List<Statement>?

    override fun createMutation() {
        // select class
        val classes = allOfType(owlClass)
        val class1 = classes.randomOrNull(randomGenerator)

        // crate complex class expression
        val class2 = model.createResource()
        val classExpression= getComplexClassExpression(class2)

        if (class1 != null && classExpression != null) {
            // select randomly, which class is subclass
            if (randomGenerator.nextBoolean())
                addSet.add(model.createStatement(class1, subClassProp, class2))
            else
                addSet.add(model.createStatement(class2, subClassProp, class1))

            addSet.addAll(classExpression)
        }
        super.createMutation()
    }
}


class DeclareClassMutation(model: Model) : DeclareObjectOfTypeMutation(model) {
    override val targetType = owlClass
    override val prefix: String
        get() = "newOwlClass"
}

class DeclareObjectPropMutation(model: Model) : DeclareObjectOfTypeMutation(model) {
    override val targetType = objectPropClass
    override val prefix: String
        get() = "newObjectProp"
}

class DeclareDataPropMutation(model: Model) : DeclareObjectOfTypeMutation(model) {
    override val targetType = dataPropClass
    override val prefix: String
        get() = "newDataProp"
}

//removes one (random) subclass axiom       // val m = Mutator
class RemoveSubclassRelationMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = subClassProp
}

class AddSubclassRelationMutation(model: Model) : AddClassRelationMutation(model) {
    override val addedRelation = subClassProp
}

class AddEquivalentClassRelationMutation(model: Model) : AddClassRelationMutation(model) {
    override val addedRelation = equivClassProp
}

//removes one (random) equivClass axiom       // val m = Mutator
class RemoveEquivClassRelationMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = equivClassProp
}

class AddDisjointClassRelationMutation(model: Model) : AddClassRelationMutation(model) {
    override val addedRelation = disjointClassProp
}

class RemoveDisjointClassRelationMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = disjointClassProp
}

class AddReflexiveObjectPropertyRelationMutation(model: Model) : AddTypeInformationMutation(model) {
    override val additionalType = reflexiveProp
    override val targetObjects = objectPropClass
}

class AddTransitiveObjectPropertyRelationMutation(model: Model) : AddTypeInformationMutation(model) {
    override val additionalType = transitiveProp
    override val targetObjects = objectPropClass
}

class AddObjectPropDomainMutation(model: Model) : AddObjectPropClassRelationMutation(model) {
    override val addedRelation = domainProp
}

class AddDataPropDomainMutation(model: Model) : AddDataPropClassRelationMutation(model) {
    override val addedRelation = domainProp
}

class RemoveDomainRelationMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = domainProp
}

class AddObjectPropRangeMutation(model: Model) : AddObjectPropClassRelationMutation(model) {
    override val addedRelation = rangeProp
}

// add a range to a data property
class AddDataPropRangeMutation(model: Model) : AddStatementMutation(model) {
    override fun createMutation() {
        val sources = allOfType(dataPropClass)
        val targets = allElDataTypes

        if (sources.isNotEmpty()) {
            val source = sources.random(randomGenerator)
            val target = targets.random(randomGenerator)

            val s = model.createStatement(
                source,
                rangeProp,
                target
            )
            val config = SingleStatementConfiguration(s)
            super.setConfiguration(config)
        }

        super.createMutation()
    }
}

class RemoveRangeRelationMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = rangeProp
}

class AddSubObjectPropMutation(model: Model): AddRelationBetweenObjectPropMutation(model) {
    override val addedRelation = subPropertyProp
}

class AddSubDataPropMutation(model: Model) : AddRelationBetweenDataPropMutation(model) {
    override val addedRelation = subPropertyProp
}

class RemoveSubPropMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = subPropertyProp
}

class AddEquivObjectPropMutation(model: Model): AddRelationBetweenObjectPropMutation(model) {
    override val addedRelation = equivPropertyProp
}

class AddEquivDataPropMutation(model: Model) : AddRelationBetweenDataPropMutation(model) {
    override val addedRelation = equivPropertyProp
}

class RemoveEquivPropMutation(model: Model) : RemoveStatementByRelationMutation(model) {
    override val targetPredicate = equivPropertyProp
}

class AddPropertyChainMutation(model: Model) : Mutation(model) {
    val properties =allOfType(objectPropClass)

    override fun isApplicable(): Boolean {
        return properties.isNotEmpty()
    }

    override fun createMutation() {
        if (isApplicable()){
            // select links in chain and super property
            val superProp = properties.random(randomGenerator)
            val link1 = properties.random(randomGenerator)
            val link2 = properties.random(randomGenerator)

            val result = ComplexStatementBuilder(model).propertyChain(listOf(link1, link2), superProp)
            for (s in result)
                addSet.add(s)

        }
        super.createMutation()
    }
}

// removes one part of an "AND" in a logical axiom
class CEUAMutation(model: Model): ReplaceNodeInStatementMutation(model)   {
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
class CEUOMutation(model: Model): ReplaceNodeInStatementMutation(model)   {

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
class ACATOMutation(model: Model): ReplaceNodeInStatementMutation(model) {

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
class ACOTAMutation(model: Model): ReplaceNodeInStatementMutation(model) {
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
class ToSiblingClassMutation(model: Model): ReplaceNodeInStatementMutation(model) {

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
            if (c.toString() != cSibling.toString()) { // check, if entities different
                val axiom = model.createStatement(restriction.asResource(),someValuesFromProp, c)
                ret += DoubleStringAndStatementConfiguration(
                    c.toString(),
                    cSibling.toString(),
                    axiom)
            }
        }

        return ret.sortedBy { it.toString() }
    }

}

class RemoveClassMutation(model: Model) : RemoveNodeMutation(model) {
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

class RemoveObjectPropertyMutation(model: Model) : RemoveNodeMutation(model) {
    override fun getCandidates(): List<Resource> {
        val l = model.listStatements().toList().toMutableList()
        val candidates = ArrayList<Resource>()
        for (s in l) {
            // check, if statement is object property declaration
            if (s.predicate == rdfTypeProp && s.`object` == objectPropClass) {
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

class ReplaceClassWithTopMutation(model: Model) : ReplaceNodeWithNode(model) {
    override fun createMutation() {
        // select a random class to be replaced
        // ignore classes that share their name with properties
        val classes = allOfType(owlClass).minus(allOfType(objectPropClass)).minus(allOfType(dataPropClass))

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

class ReplaceClassWithBottomMutation(model: Model) : ReplaceNodeWithNode(model) {
    override fun createMutation() {
        // select a random class to be replaced
        // ignore classes that share their name with properties
        val classes = allOfType(owlClass).minus(allOfType(objectPropClass)).minus(allOfType(dataPropClass))
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

class ReplaceClassWithSiblingMutation(model: Model): ReplaceNodeWithNode(model) {
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

/// mutations to add complex class axioms
/// adds subclass axiom with "objectIntersectionOf" expression
class AddObjectIntersectionOfMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val classes = allOfType(owlClass)
        if (classes.isNotEmpty()) {
            // randomly: 2–5 classes in intersection
            val numberOfClasses = randomGenerator.nextInt(2,5)
            val intersectionClasses = mutableListOf<Resource>()
            for (i in 1..numberOfClasses)
                intersectionClasses.add(classes.random(randomGenerator))

            return statementBuilder.objectIntersectionOf(head, intersectionClasses)
        }
        // can not create --> emp
        return null
    }
}

/// adds subclass axiom with "objectOneOf" expression; according to EL profile
class AddELObjectOneOfMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val individuals = allOfType(namedInd)
        if (individuals.isNotEmpty()) {
            return statementBuilder.objectOneOf(head, listOf(individuals.random(randomGenerator)))
        }
        // can not create --> emp
        return null
    }
}

/// adds subclass axiom with "objectSomeValuesFrom" expression; according to EL profile
class AddObjectSomeValuesFromMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val classes = allOfType(owlClass)
        val properties = allOfType(objectPropClass)
        if (classes.isNotEmpty() && properties.isNotEmpty()) {
            return statementBuilder.someValuesFrom(
                head,
                properties.random(randomGenerator),
                classes.random(randomGenerator)
            )
        }
        // can not create --> return null
        return null
    }
}

/// adds subclass axiom with "dataIntersectionOf" expression; tailored towards EL profile
class AddELDataIntersectionOfMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val result = mutableListOf<Statement>()
        val dataProperties = allOfType(dataPropClass)
        if (dataProperties.isNotEmpty()) {
            val dataProperty = dataProperties.random(randomGenerator)

            // randomly: 2–5 classes in intersection
            val numberOfRanges = randomGenerator.nextInt(2,3)
            val intersectionRanges = mutableListOf<Resource>()
            for (i in 1..numberOfRanges)
                intersectionRanges.add(allElDataTypes.random(randomGenerator))

            // create data intersection
            val intersectionHead = model.createResource()
            result.addAll(statementBuilder.dataIntersectionOf(intersectionHead, intersectionRanges))

            // encapsulated in "someValuesFrom" construct
            result.addAll(statementBuilder.someValuesFrom(
                head,
                dataProperty,
                intersectionHead
                ))
            return result
        }
        // can not create --> emp
        return null
    }
}

/// adds subclass axiom with "dataOneOf" expression; tailored towards EL profile
class AddELDataOneOfMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val result = mutableListOf<Statement>()
        val dataProperties = allOfType(dataPropClass)
        if (dataProperties.isNotEmpty()) {
            val dataProperty = dataProperties.random(randomGenerator)

            // create data one of (only one element, as we are in EL profile)
            val oneOfHead = model.createResource()
            result.addAll(statementBuilder.dataOneOf(oneOfHead, listOf(exampleElDataValues.random(randomGenerator))))

            // encapsulated in "someValuesFrom" construct
            result.addAll(statementBuilder.someValuesFrom(
                head,
                dataProperty,
                oneOfHead
            ))
            return result
        }
        // can not create --> emp
        return null
    }
}

/// adds subclass axiom with "DataSomeValuesFrom" expression with simple data range; tailored towards EL profile
class AddELSimpleDataSomeValuesFromMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val result = mutableListOf<Statement>()
        val dataProperties = allOfType(dataPropClass)
        if (dataProperties.isNotEmpty()) {
            val dataProperty = dataProperties.random(randomGenerator)

            // encapsulated in "someValuesFrom" construct
            result.addAll(statementBuilder.someValuesFrom(
                head,
                dataProperty,
                allElDataTypes.random(randomGenerator)
            ))
            return result
        }
        // can not create --> emp
        return null
    }
}

/// adds subclass axiom with "objectHasValue" expression; according to EL profile
class AddObjectHasValueMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val individuals = allOfType(namedInd)
        val properties = allOfType(objectPropClass)
        if (individuals.isNotEmpty() && properties.isNotEmpty()) {
            return statementBuilder.objectHasValue(
                head,
                properties.random(randomGenerator),
                individuals.random(randomGenerator)
            )
        }
        // can not create --> return null
        return null
    }
}

/// adds subclass axiom with "objectHasValue" expression; according to EL profile
class AddDataHasValueMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val properties = allOfType(dataPropClass)
        if (properties.isNotEmpty()) {
            return statementBuilder.dataHasValue(
                head,
                properties.random(randomGenerator),
                exampleElDataValues.random(randomGenerator)
            )
        }
        // can not create --> return null
        return null
    }
}

/// adds subclass axiom with "objectHasSelf" expression; according to EL profile
class AddObjectHasSelfMutation(model: Model) : AddComplexSubClassAxiomMutation(model) {
    override fun getComplexClassExpression(head: Resource): List<Statement>? {
        val properties = allOfType(objectPropClass)
        if (properties.isNotEmpty()) {
            return statementBuilder.objectHasSelf(
                head,
                properties.random(randomGenerator)
            )
        }
        // can not create --> return null
        return null
    }
}

// adds datatype definition
// note: only considers "dataOneOf" with one element --> only one special case
class AddDatatypeDefinition(model: Model) : Mutation(model) {
    override fun createMutation() {
        val newDatatype = model.createResource("newDatatype:" + randomGenerator.nextInt())
        val definitionHead = model.createResource()

        addSet.add(model.createStatement(newDatatype, equivClassProp, definitionHead))
        addSet.addAll(statementBuilder.dataOneOf(
            definitionHead,
            exampleElDataValues.random(randomGenerator)
        ))
        super.createMutation()
    }
}

// adds "hasKey" axiom
// simplification: only with object properties
class AddHasKeyMutation(model: Model) : Mutation(model) {
    override fun createMutation() {
        val targetClass = allOfType(owlClass).randomOrNull(randomGenerator)
        val objectProps = allOfType(objectPropClass)
        if (targetClass != null && objectProps.isNotEmpty()){
            val numberOfProps = randomGenerator.nextInt(1,5)
            val propList = mutableListOf<Resource>()
            for (i in 1..numberOfProps)
                propList.add(objectProps.random(randomGenerator))

            val listHead = model.createResource()
            addSet.add(model.createStatement(targetClass, hasKey, listHead))
            addSet.addAll(statementBuilder.sequenceOf(listHead, propList))
        }

        super.createMutation()
    }
}