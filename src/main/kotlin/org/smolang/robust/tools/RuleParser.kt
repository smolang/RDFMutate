package org.smolang.robust.tools

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.apache.jena.vocabulary.OWL
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
            if (config != null && (config.head.any() || config.body.any()))
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
    private fun parseSwrlRule(rootNode : Resource, variables : Set<Resource>) : RuleMutationConfiguration? {
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

        val headAtoms = parseAtomList(head) ?: return null
        val bodyAtoms = parseAtomList(body) ?: return null

        // get variables of head and body
        val headVariables = variables.filter { v -> containsResource(headAtoms, v) }.toSet()
        val bodyVariables = variables.filter { v -> containsResource(bodyAtoms, v) }.toSet()

        return RuleMutationConfiguration(bodyAtoms, headAtoms, bodyVariables, headVariables)
    }

    // parses the atom list from an RDF file
    private fun parseAtomList(listRoot : Resource) : List<MutationAtom>? {
        val result = mutableListOf<MutationAtom>()

        // parse the first element
        val listHead = model.listObjectsOfProperty(listRoot, RDF.first).toSet().single().asResource()
        val headStatement = parseSWRLAtom(listHead) ?: return null

        // only add the elements that could be parsed, i.e. are not null

        result.add(headStatement)

        // check, if there are more elements to parse
        if (!model.listStatements(listRoot, RDF.rest, RDF.nil).hasNext()) {
            val listRest = model.listObjectsOfProperty(listRoot, RDF.rest).toSet().single().asResource()
            // check, if type is correct
            assert(hasType(listRest, SWRL.AtomList))
            val parsedRest = parseAtomList(listRest) ?: return null
            result.addAll(parsedRest)
        }

        return result
    }

    // parses an SWRL atom
    private fun parseSWRLAtom(atomRoot : Resource) : MutationAtom? {
        val type = model.listObjectsOfProperty(atomRoot, RDF.type).toSet().single()
        return when (type) {
            SWRL.IndividualPropertyAtom -> parsePropertyAtom(atomRoot)
            SWRL.ClassAtom -> parseClassAtom(atomRoot)
            SWRL.DatavaluedPropertyAtom -> parsePropertyAtom(atomRoot)
            SWRL.BuiltinAtom -> parseBuiltinAtom(atomRoot)
            else -> {
                mainLogger.warn("Atom type of SWRL atom $atomRoot is not supported and can not be parsed.")
                null
            }
        }
    }

    private fun parsePropertyAtom(root : Resource) : StatementAtom? {
        assert(hasType(root, SWRL.IndividualPropertyAtom) || hasType(root, SWRL.DatavaluedPropertyAtom))
        val subject = model.listObjectsOfProperty(root, SWRL.argument1).toSet().singleOrNull()?.asResource()
        val SWRLobject = model.listObjectsOfProperty(root, SWRL.argument2).toSet().singleOrNull()
        val property = model.listObjectsOfProperty(root, SWRL.propertyPredicate).toSet().singleOrNull()

        // check, if all elements could be extracted
        if (subject == null || SWRLobject == null || property == null) {
            mainLogger.warn("Could not parse SWRL IndividualPropertyAtom! Please check the syntax of SWRL rule.")
            return null
        }

        val propertyStatement = model.createStatement(
            subject,
            model.getProperty(property.toString()),
            SWRLobject)

        return PositiveStatementAtom(propertyStatement)
    }

    private fun parseClassAtom(root : Resource) : StatementAtom? {
        assert(hasType(root, SWRL.ClassAtom))
        val subject = model.listObjectsOfProperty(root, SWRL.argument1).toSet().singleOrNull()?.asResource()
        val SwrlClass = model.listObjectsOfProperty(root, SWRL.classPredicate).toSet().singleOrNull()?.asResource()

        // check, if all elements could be extracted
        if (subject == null || SwrlClass == null) {
            mainLogger.warn("Could not parse SWRL ClassAtom! Please check the syntax of SWRL rule.")
            return null
        }

        val classStatement = model.createStatement(
            subject,
            RDF.type,
            SwrlClass
        )

        return PositiveStatementAtom(classStatement)
    }

    private fun parseBuiltinAtom(root: Resource) : StatementAtom? {
        assert(hasType(root, SWRL.BuiltinAtom))
        val type = model.listObjectsOfProperty(root, SWRL.builtin).toSet().single()
        return when (type){
            OWL.NegativePropertyAssertion -> parseNegativePropertyAtom(root)
            else ->{
                mainLogger.warn("Builtin SWRL atom of type $type is not supported and can not be parsed.")
                null
            }
        }

    }

    private fun parseNegativePropertyAtom(root: Resource) : StatementAtom? {
        val argumentsHead = model.listObjectsOfProperty(root, SWRL.arguments).toSet().singleOrNull()?.asResource()
        if (argumentsHead == null) {
            mainLogger.warn("Builtin SWRL atom that reflects negative property assertion is not correctly structured" +
                    " and can not be parsed. Not exactly one \"swrl:arguments\" argument.")
            return null
        }

        val arguments = ComplexTermParser().allElementsInList(model, argumentsHead)
        if (arguments.size != 3) {
            mainLogger.warn("Builtin SWRL atom that reflects negative property assertion is not correctly structured" +
                    " and can not be parsed. Not the correct number of arguments (${arguments.size} arguments " +
                    "but 3 required).")
            return null
        }
        val negatedStatement = model.createStatement(
            arguments[0].asResource(),
            model.getProperty(arguments[1].toString()),
            arguments[2]
        )

        return NegativeStatementAtom(negatedStatement)
    }

    // check type declaration of a resource
    private fun hasType(r : Resource, type : Resource) : Boolean {
        return model.listStatements(r, RDF.type, type).hasNext()
    }

    // checks, if the item is contained in the list of statements
    private fun containsResource(atoms : List<MutationAtom>, item : Resource) : Boolean {
        for (a in atoms)
            if (a.containsResource(item))
                return true

        return false
    }



}