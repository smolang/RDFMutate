package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SWRL

import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration

// a class to parse rules, i.e., SWRL rules, to create mutations
class RuleParser(val model: Model) {

    fun getAllRuleMutations() : List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()
        val variables = getSwrlVariables()
        val rules = model.listSubjectsWithProperty(RDF.type, SWRL.Imp)

        // add one abstract mutation for each rule
        for (rule in rules) {
            val config = parseSwrlRule(rule, variables)
            // check, if a rule with some content was parsed
            if (config.head.any() || config.body.any())
                abstractMutations.add(AbstractMutation(RuleMutation::class, config))
        }
        return abstractMutations
    }

    // returns all swrl variables in model
    private fun getSwrlVariables() : Set<Resource> {
        return model.listSubjectsWithProperty(RDF.type, SWRL.Variable).toSet()
    }

    // parses a SWRL rule into a rule mutation
    // rootNode: root of the SWRL rule
    private fun parseSwrlRule(rootNode : Resource, variables : Set<Resource>) : RuleMutationConfiguration {
        // check type that root is really correct
        if (!hasType(rootNode, SWRL.Imp)) {
            mainLogger.warn("The provided root $rootNode node does not identify a SWRL rule. " +
                    "Fallback: return empty mutation")
            return  RuleMutationConfiguration()
        }

        // get the nodes for the head and the body
        val head = model.listStatements(rootNode, SWRL.head, null as RDFNode?)
            .toSet().single().`object`.asResource()
        val body = model.listStatements(rootNode, SWRL.body, null as RDFNode?)
            .toSet().single().`object`.asResource()
        // check correct types
        assert(hasType(head, SWRL.AtomList))
        assert(hasType(body, SWRL.AtomList))

        val headAtoms = parseAtomList(head)
        val bodyAtoms = parseAtomList(body)

        // get variables of head and body
        val headVariables = variables.filter { v -> containsResource(headAtoms, v) }.toSet()
        val bodyVariables = variables.filter { v -> containsResource(bodyAtoms, v) }.toSet()

        return RuleMutationConfiguration(bodyAtoms, headAtoms, bodyVariables, headVariables)
    }

    // parses the atom list from an RDF file
    private fun parseAtomList(listRoot : Resource) : List<Statement> {
        val result = mutableListOf<Statement>()

        // parse the first element
        val listHead = model.listObjectsOfProperty(listRoot, RDF.first).toSet().single().asResource()
        val headStatement = parseSWRLAtom(listHead)

        // only add the elements that could be parsed, i.e. are not null
        if (headStatement!= null)
            result.add(headStatement)

        // check, if there are more elements to parse
        if (!model.listStatements(listRoot, RDF.rest, RDF.nil).hasNext()) {
            val listRest = model.listObjectsOfProperty(listRoot, RDF.rest).toSet().single().asResource()
            // check, if type is correct
            assert(hasType(listRest, SWRL.AtomList))
            result.addAll(parseAtomList(listRest))
        }

        return result
    }

    // parses an SWRL atom
    private fun parseSWRLAtom(atomRoot : Resource) : Statement? {
        val type = model.listObjectsOfProperty(atomRoot, RDF.type).toSet().single()
        return when (type) {
            SWRL.IndividualPropertyAtom -> parseIndividualPropertyAtom(atomRoot)
            SWRL.ClassAtom -> parseClassAtom(atomRoot)
            else -> {
                mainLogger.warn("Atom type of SWRL atom $atomRoot is not supported and can not be parsed.")
                null
            }
        }
    }

    private fun parseIndividualPropertyAtom(root : Resource) : Statement? {
        assert(hasType(root, SWRL.IndividualPropertyAtom))
        val subject = model.listObjectsOfProperty(root, SWRL.argument1).toSet().singleOrNull()?.asResource()
        val SWRLobject = model.listObjectsOfProperty(root, SWRL.argument2).toSet().singleOrNull()?.asResource()
        val property = model.listObjectsOfProperty(root, SWRL.propertyPredicate).toSet().singleOrNull()

        // check, if all elements could be extracted
        if (subject == null || SWRLobject == null || property == null) {
            mainLogger.warn("Could not parse SWRL IndividualPropertyAtom! Please check the syntax of SWRL rule.")
            return null
        }

        return model.createStatement(
            subject,
            model.getProperty(property.toString()),
            SWRLobject)
    }

    private fun parseClassAtom(root : Resource) : Statement? {
        assert(hasType(root, SWRL.ClassAtom))
        val subject = model.listObjectsOfProperty(root, SWRL.argument1).toSet().singleOrNull()?.asResource()
        val SwrlClass = model.listObjectsOfProperty(root, SWRL.classPredicate).toSet().singleOrNull()?.asResource()

        // check, if all elements could be extracted
        if (subject == null || SwrlClass == null) {
            mainLogger.warn("Could not parse SWRL ClassAtom! Please check the syntax of SWRL rule.")
            return null
        }

        return model.createStatement(
            subject,
            RDF.type,
            SwrlClass
        )
    }

    // check type declaration of a resource
    private fun hasType(r : Resource, type : Resource) : Boolean {
        return model.listStatements(r, RDF.type, type).hasNext()
    }

    // checks, if the item is contained in the list of statements
    private fun containsResource(statements : List<Statement>, item : Resource) : Boolean {
        for (s in statements)
            if (containsResource(s, item))
                return true

        return false
    }

    // check, if statement contains item
    private fun containsResource(s : Statement, item : Resource) : Boolean {
        return s.subject == item || s.predicate == item ||
                (s.`object`.isResource && s.`object`.asResource() == item)
    }

}