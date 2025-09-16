package org.smolang.robust.tools

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.patterns.PatternExtractor
import org.smolang.robust.randomGenerator
import org.smolang.robust.tools.ruleMutations.FreshNodeAtom
import org.smolang.robust.tools.ruleMutations.MutationAtom
import org.smolang.robust.tools.ruleMutations.NegativeStatementAtom
import org.smolang.robust.tools.ruleMutations.PositiveStatementAtom
import java.io.File
import java.nio.file.Files
import kotlin.math.absoluteValue
import kotlin.time.measureTime

class OperatorLearner(val verbose: Boolean =false) {

    // local map of common prefixes
    val prefixMap: Map<String, String> = mapOf(
        "rdf:" to RDF.getURI(),
        "rdfs:" to RDFS.getURI(),
        "owl:" to OWL.getURI(),
        "xsd:" to XSD.getURI(),
        "swrl:" to "http://www.w3.org/2003/11/swrl#",
        "swrla:" to "http://swrl.stanford.edu/ontologies/3.3/swrla.owl#",
        "swrlb:" to "http://www.w3.org/2003/11/swrlb#",
        "mros:" to "http://ros/mros#",
        "suave:" to "http://www.metacontrol.org/suave#",
        "tomasys:" to "http://metacontrol.org/tomasys#"
    )

    fun mineELRules(outputFile: File) {
        val orePatternExtractor = PatternExtractor(
            50,
            20,
            0.8,
            3
        )

        // get all files from folder
        val directory = File("sut/reasoners/ontologies_ore")
        // filter for files that end with ".owl"
        val filesInDirectory = Files.walk(directory.toPath())
            .filter { path -> path.toString().endsWith(".owl") }
            .toList()
            .map { path -> path.toFile() }
            .toSet()

        println("found ${filesInDirectory.size} files in ORE-EL directory")
        println("start mining EL rules")

        mineRules(orePatternExtractor, filesInDirectory, outputFile)

        println("finished mining EL rules")
        println()
    }

    fun mineSuaveRules(outputFile: File) {
        println("start mining SAUVE rules")
        val suavePatternExtractor = PatternExtractor(
            10,
            5,
            0.8,
            3
        )
        val files = setOf(File("sut/suave/suave_ontologies/suave_original.owl"))

        mineRules(suavePatternExtractor, files, outputFile)

        println("finished mining EL rules")
        println()
    }

    fun mineRules(
        ruleExtractor: PatternExtractor,
        inputFiles: Set<File>,
        outputFile: File
    ) {
        val miningTime = measureTime {
            val rules = ruleExtractor.extractRules(inputFiles)
            println("mined ${rules.size()} rules in total")
            // save rules: also important to load operators from them
            ruleExtractor.saveRulesToFile(rules, outputFile)
        }

        println("mining rules took ${miningTime.inWholeSeconds}s ")
    }

    // loads all rules form one file and creates abstract mutations for them
    fun rulesToAbstractMutation(rulesFile: File): List<AbstractMutation> {
        val mutationOperators: List<AbstractMutation> = rulesFile.useLines { lines ->
            lines.flatMap {line ->
                //println("parse line \"$line\"")
                ruleToAbstractMutations(line)
            }.toList()
        }
        return mutationOperators
    }

    // parses one rule into a configuration
    // one rule results in multiple configurations, depending on how many of the body atoms match
    fun ruleToAbstractMutations(rule: String): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()
        // extract variables
        //println("Extracting operators from rules \"$rule\"")
        val variables = getVariables(rule)
        //println("vars: $variables")

        // get representation of string as atoms
        val rule = getRule(rule, variables)

        abstractMutations.addAll(getAddingOperators(rule, variables))
        abstractMutations.addAll(getDeletingOperators(rule, variables))

        return abstractMutations
    }

    // creates all the operators that only add new triples to graph
    private fun getAddingOperators(rule: AssociationRule, variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are newly added VS searched for in KG
        val newBodyAtomCombinations = subsets(rule.bodyAtoms)
        //println(newBodyAtomCombinations)

        newBodyAtomCombinations.forEach { newBodyAtoms ->
            // build new head; add body atoms that are newly added
            val mutationHead = mutableListOf(rule.headAtom)
            rule.bodyAtoms.forEach { atom ->
                if (newBodyAtoms.contains(atom)) mutationHead.add(atom)
            }
            val headVariables = containedVars(mutationHead, variables)

            // identify elements that remain in body
            val mutationBody = rule.bodyAtoms.filter { atom -> !newBodyAtoms.contains(atom) }.toMutableList()
            val bodyVariables = containedVars(mutationBody, variables)

            // add declarations as new nodes for variables that do not occur in body anymore but in head
            val freshNodes = headVariables.minus(bodyVariables)
            //println(freshNodes)
            freshNodes.forEach { freshNode ->
                mutationBody.add(FreshNodeAtom(freshNode))
            }

            // assert that body covers at least all head variables
            val finalBodyVariables = containedVars(mutationBody, variables)
            assert(headVariables.minus(finalBodyVariables).isEmpty()) {"ERROR: something went wrong"}


            // build configuration
            val config = RuleMutationConfiguration(
                mutationBody,
                mutationHead,
                finalBodyVariables,
                headVariables
            )

            // build abstract mutation and add to list
            abstractMutations.add(AbstractMutation(RuleMutation::class, config, verbose))
        }
        return abstractMutations
    }

    // creates all the operators that only delete triples from the graph
    private fun getDeletingOperators(rule: AssociationRule, variables: Map<String, Resource>): List<AbstractMutation> {
        val abstractMutations = mutableListOf<AbstractMutation>()

        // iterate: different body triples are deleted together with the head
        val deletedBodyAtomCombinations = subsets(rule.bodyAtoms)

        // new body for mutation
        val mutationBody = rule.bodyAtoms.toMutableList()
        mutationBody.add(rule.headAtom)
        val bodyVariables = containedVars(mutationBody, variables)

        deletedBodyAtomCombinations.forEach { deletedBodyAtoms ->
            // only create mutation operator if set of selected body atoms is not empty
            val mutationHead = mutableListOf<MutationAtom>()
            deletedBodyAtoms.forEach { atom ->
                // add for each selected positive assertion a negative assertion to the head
                if (atom is PositiveStatementAtom) {
                    mutationHead.add(NegativeStatementAtom(atom.statement))
                }
            }
            if (!mutationHead.isEmpty() && rule.headAtom is PositiveStatementAtom) {
                // only if we were able to invalidate the rule body by negating at least one positive statement
                // can we safely delete the head as well and define this as an abstract mutation
                mutationHead.add(NegativeStatementAtom(rule.headAtom.statement))
                val headVariables = containedVars(mutationHead, variables)

                val config = RuleMutationConfiguration(
                    mutationBody,
                    mutationHead,
                    bodyVariables,
                    headVariables
                )
                // add abstract mutation
                abstractMutations.add(AbstractMutation(RuleMutation::class, config, verbose))
            }
        }
        return abstractMutations
    }

    // extracts set of variables from string representing a rule
    // i.e., extract all elements starting with "?"
    // represent variables as mapping from strings to resources
    fun getVariables(s: String): Map<String, Resource> {
        val regex = "\\?.".toRegex()
        val stringVars= regex.findAll(s).map { it.value }.toSet()
        // random number to avoid using same IRI for variable names that are occur in multiple rules
        val varID = randomGenerator.nextInt().absoluteValue

        // map string representations of variables to Resources
        return stringVars.associateWith { s ->
            ResourceFactory.createResource(
                "${MutationAtom.MUTATE_PREFIX}variable${s.removePrefix("?")}"//$varID"
            )
        }
    }

    fun getRule(rule: String, variables: Map<String, Resource>): AssociationRule {
        val implication = "->".toRegex()

        // there should be exactly one arrow in the rule
        assert(
            implication.findAll(rule).toSet().size == 1
        ) { "rule patterns need to contain exactly one implication arrow \"->\"" }
        val split = rule.split("->")
        val body = split[0]
        val head = split[1]

        val atom = "\\(([^)]*)\\)".toRegex()
        val bodyAtomsStrings = atom.findAll(body).map{ r -> r.value}.toList()
        val headAtomString = atom.findAll(head).map{r -> r.value}.toList().single()

        //println("body atoms: $bodyAtoms")
        //println("head atom: $headAtom")

        val bodyAtoms =  bodyAtomsStrings.map { a ->
            getAtom(a, variables)
        }
        val headAtom = getAtom(headAtomString, variables)
        return AssociationRule(bodyAtoms, headAtom)
    }

    // turn string representation of atom into atom based on JENA resources
    fun getAtom(ruleAtom: String, variables: Map<String, Resource>): MutationAtom {
        val elements = ruleAtom.split(" ".toRegex())
        assert(elements.size == 3) {"atoms need to contain exactly three elements, i.e., represent triples"}
        val s = parseNode(elements[0].removePrefix("("), variables)
        val p = parseNode(elements[1], variables)
        val o = parseNode(elements[2].removeSuffix(")"), variables)
        assert(s.isResource)
        assert(p.isResource)
        val statement = ResourceFactory.createStatement(
            s.asResource(), ResourceFactory.createProperty(p.toString()), o
        )
        return PositiveStatementAtom(statement)
    }

    // parses string to resource
    fun parseNode(s: String, variables: Map<String, Resource>): RDFNode {
        if (variables.containsKey(s))
            return variables[s]!!



        if (s.indexOfFirst { c -> c == ':' } > -1) {
            // handle prefix
            val p = s.substringBefore(':') + ":"
            val rest = s.substringAfter(':')
            if (prefixMap.containsKey(p)) {
                return ResourceFactory.createResource(prefixMap[p] + rest)
            }
            else
                println("WARNING: can not find prefix $p in prefix map")
        }
        return ResourceFactory.createResource(s)
    }

    // produces all subsets for provided set
    fun <T> subsets(elements: List<T>): List<List<T>> {
        // add empty set
        if (elements.isEmpty())
            return listOf(listOf()) // return set with empty set

        // there is at least an element
        val head = elements.first()
        val rest = elements.filter { e -> e != head }

        // recursive call
        val recursiveSets = subsets(rest)

        val subsets: MutableList<List<T>> = mutableListOf()
        recursiveSets.forEach { set ->
            subsets.add(set)
            val extendedSet = mutableListOf<T>()
            set.forEach { e -> extendedSet.add(e)}
            extendedSet.add(head)
            subsets.add(extendedSet)
        }

        return  subsets
    }

    // returns the variables (as Resources) that are contained in a list of atoms
    fun containedVars(atoms: List<MutationAtom>, variables: Map<String, Resource>): Set<Resource> {
        // collect all variables from all atoms
        return atoms.flatMap { atom ->
            variables.values.filter { variable -> atom.containsResource(variable) }
        }.toSet()
    }
}

// represents a mined rule: list of atoms as body and a single atom as head
data class AssociationRule(
    val bodyAtoms: List<MutationAtom>,
    val headAtom: MutationAtom
) {
    override fun toString(): String {
        val body = bodyAtoms.fold("", {s, a -> "$s,${a.toLocalString()}"})
        return "AssociationRule(body=$body, head=${headAtom.toLocalString()})"
    }
}