package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SWRL

import org.smolang.robust.mainLogger
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.tools.ComplexTermParser
import org.smolang.robust.tools.MutationFileParser
import java.io.File

// a class to parse rules, i.e., SWRL rules, to create mutations
class SWRLRuleParser(val file: File) : MutationFileParser() {

    val parsedModel = loadModel(file)
    val model = parsedModel.model

    // main method to extract abstract mutations
    override fun getAllAbstractMutations() : List<AbstractMutation>? {
        val abstractMutations = mutableListOf<AbstractMutation>()
        if (!parsedModel.successful)  // return null, if file could not be parsed
            return null

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

    // loads model from file
    private fun loadModel(file: File) : RuleParsingResult {

        if(!file.absoluteFile.exists()){
            mainLogger.error("File ${file.path} for mutations does not exist")
            return RuleParsingResult(ModelFactory.createDefaultModel(), false)
        } else  {
            val input = try {
                RDFDataMgr.loadDataset(file.absolutePath).defaultModel
            } catch (e : Exception) {
                mainLogger.error("Could not open file $file. Following exception occurred: $e")
                return RuleParsingResult(ModelFactory.createDefaultModel(), false)
            }
            return RuleParsingResult(input, true)
        }
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
            mainLogger.warn("The provided root $rootNode node does not identify a SWRL rule. ")
            return null
        }

        // get the nodes for the head and the body
        val head = model.listStatements(rootNode, SWRL.head, null as RDFNode?)
            .toSet().single().`object`.asResource()
        val body = model.listStatements(rootNode, SWRL.body, null as RDFNode?)
            .toSet().single().`object`.asResource()

        // check correct types
        //assert(hasType(head, SWRL.AtomList))
        //assert(hasType(body, SWRL.AtomList))

        val headAtoms = parseAtomList(head) ?: return null
        val bodyAtoms = parseAtomList(body) ?: return null

        // check constraints on atoms
        if (!validMutationRule(bodyAtoms, headAtoms))
            return null

        // get variables of head and body
        val headVariables = variables.filter { v -> containsResource(headAtoms, v) }.toSet()
        val bodyVariables = variables.filter { v -> containsResource(bodyAtoms, v) }.toSet()

        return RuleMutationConfiguration(bodyAtoms, headAtoms, bodyVariables, headVariables)
    }

    // parses the atom list from an RDF file
    // returns "null" if any of the atoms can not be parsed correctly
    private fun parseAtomList(listRoot : Resource) : List<MutationAtom>? {
        val result = mutableListOf<MutationAtom>()

        // empty list
        if (listRoot == RDF.nil)
            return listOf()

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

        val isDataProp = hasType(root, SWRL.DatavaluedPropertyAtom)
        return PositiveStatementAtom(propertyStatement, isDataProp)
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

    private fun parseBuiltinAtom(root: Resource) : MutationAtom? {
        assert(hasType(root, SWRL.BuiltinAtom))

        val type = try {
            model.listObjectsOfProperty(root, SWRL.builtin).toSet().single()
        } catch (_: Exception) {
            mainLogger.warn("Builtin for rule mutation can not be parsed because not exactly one ")
            return null
        }

        return when (type){
            model.getResource(NegativeStatementAtom.BUILTIN_IRI) -> parseNegativePropertyAtom(root)
            model.getResource(FreshNodeAtom.BUILTIN_IRI) -> parseFreshNodeAtom(root)
            model.getResource(DeleteNodeAtom.BUILTIN_IRI) -> parseDeleteNodeAtom(root)
            model.getResource(ReplaceNodeAtom.BUILTIN_IRI) -> parseReplaceNodeAtom(root)
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
                    "provided but 3 required).")
            return null
        }


        val negatedStatement = model.createStatement(
            arguments[0].asResource(),
            model.getProperty(arguments[1].toString()),
            resourceOrNode(arguments[2])         // check if last argument is resource or not

        )

        return NegativeStatementAtom(negatedStatement)
    }

    // parses atom declaring a fresh node
    private fun parseFreshNodeAtom(root: Resource) : FreshNodeAtom? {
        val argumentsHead = model.listObjectsOfProperty(root, SWRL.arguments).toSet().singleOrNull()?.asResource()
        if (argumentsHead == null) {
            mainLogger.warn("Builtin SWRL atom that reflects new node declaration is not correctly structured" +
                    " and can not be parsed. Not exactly one \"swrl:arguments\" argument.")
            return null
        }

        val arguments = ComplexTermParser().allElementsInList(model, argumentsHead)
        if (arguments.size != 1) {
            mainLogger.warn("Builtin SWRL atom that reflects new node declaration is not correctly structured" +
                    " and can not be parsed. Not the correct number of arguments (${arguments.size} arguments " +
                    "provided but 1 required).")
            return null
        }

        val freshNodeName = arguments[0].asResource()

        return FreshNodeAtom(freshNodeName)

    }

    // parses atom for deleting a node
    private fun parseDeleteNodeAtom(root: Resource) : DeleteNodeAtom? {
        val argumentsHead = model.listObjectsOfProperty(root, SWRL.arguments).toSet().singleOrNull()?.asResource()
        if (argumentsHead == null) {
            mainLogger.warn("Builtin SWRL atom that reflects deletion of node is not correctly structured" +
                    " and can not be parsed. Problem: Not exactly one \"swrl:arguments\" argument provided.")
            return null
        }

        val arguments = ComplexTermParser().allElementsInList(model, argumentsHead)
        if (arguments.size != 1) {
            mainLogger.warn("Builtin SWRL atom that reflects deletion of node is not correctly structured" +
                    " and can not be parsed. Not the correct number of arguments (${arguments.size} arguments " +
                    "provided but 1 required).")
            return null
        }

        val deleteNode = arguments[0]

        return DeleteNodeAtom(deleteNode)

    }

    // parses atom for deleting a node
    private fun parseReplaceNodeAtom(root: Resource) : ReplaceNodeAtom? {
        val argumentsHead = model.listObjectsOfProperty(root, SWRL.arguments).toSet().singleOrNull()?.asResource()
        if (argumentsHead == null) {
            mainLogger.warn("Builtin SWRL atom that reflects replacement of node is not correctly structured" +
                    " and can not be parsed. Problem: Not exactly one \"swrl:arguments\" argument provided.")
            return null
        }

        val arguments = ComplexTermParser().allElementsInList(model, argumentsHead)
        if (arguments.size != 2) {
            mainLogger.warn("Builtin SWRL atom that reflects replacement of node is not correctly structured" +
                    " and can not be parsed. Not the correct number of arguments (${arguments.size} arguments " +
                    "provided but 2 required).")
            return null
        }

        val oldNode = resourceOrNode(arguments[0])
        val newNode = resourceOrNode(arguments[1])

        return ReplaceNodeAtom(oldNode, newNode)

    }


    // check, if constraints on the atom structure are satisfied
    private fun validMutationRule(bodyAtoms: List<MutationAtom>, headAtoms: List<MutationAtom>): Boolean{
        // no fresh variable declaration in head --> remove if they are present
        val freshNodeHeadAtoms = headAtoms.filterIsInstance<FreshNodeAtom>()
        if (freshNodeHeadAtoms.any()) {
            mainLogger.warn("Declaration of fresh variable in head of rule is not allowed. Rule is ignored.")
            return false
        }

        // fresh nodes in body are not allowed to be used in other body atoms
        val freshNodeBodyAtoms = bodyAtoms.filterIsInstance<FreshNodeAtom>()
        freshNodeBodyAtoms.forEach { freshAtom ->
            val freshVariable = freshAtom.variable
            bodyAtoms.forEach { bodyAtom ->
                if (bodyAtom.containsResource(freshVariable) && bodyAtom != freshAtom){
                    mainLogger.warn("Variables that are new are not allowed to occur in any other atom in body of " +
                            "rule. Variable name: $freshVariable. Rule is ignored.")
                    return false
                }
            }
        }

        // delete atoms and replacement atoms are not allowed to occur in body
        bodyAtoms.forEach { atom ->
            if (atom is DeleteNodeAtom) {
                mainLogger.warn("Atoms that represent deleting of a node are not allowed to occur in body of rule." +
                        "Mutation operator can not be created. Atom with problem: ${atom.toLocalString()}")
                return false
            }
            if (atom is ReplaceNodeAtom) {
                mainLogger.warn("Atoms that represent replacement of a node are not allowed to occur in body of rule." +
                        "Mutation operator can not be created. Atom with problem: ${atom.toLocalString()}")
                return false
            }
        }

        return true
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

    // checks, if node could represent resource. If yes: return resource
    private fun resourceOrNode(node: RDFNode): RDFNode {
        if (isIRI(node.toString())) {
            mainLogger.warn("IRI as node detected. Node is cast to resource. Node: ${node}")
            return model.createResource(node.toString())
        }
        else
            return node
    }
    // checks, if string is IRI (limited capability)
    private fun isIRI(s: String): Boolean {
        return s.startsWith("http://") || s.startsWith("https://")
    }
}

data class RuleParsingResult(
    val model : Model,
    val successful : Boolean // indicates if parsing of file was successful
)