package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.patterns.PatternExtractor
import org.smolang.robust.tools.ruleMutations.FreshNodeAtom
import org.smolang.robust.tools.ruleMutations.MutationAtom
import org.smolang.robust.tools.ruleMutations.NegativeStatementAtom
import org.smolang.robust.tools.ruleMutations.PositiveStatementAtom
import java.io.File
import java.io.FileWriter
import kotlin.time.measureTime

class AssociationRuleExtractor {
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

    fun mineRules(
        ruleExtractor: PatternExtractor,
        inputFiles: Set<File>
    ) : Set<AssociationRule> {
        val extractedRules: MutableSet<AssociationRule> = mutableSetOf()
        val miningTime = measureTime {
            // TODO: extract prefix map from input files
            val associationRuleFactory = AssociationRuleFactory(prefixMap)
            ruleExtractor.extractRules(inputFiles).foreach { s ->
                val variables = AssociationRule.getVariables(s) // extract variables from mined rule
                // add association rule representing mined string rule
                extractedRules.add(
                    associationRuleFactory.getAssociationRule(s, variables)
                )
            }
            println("mined ${extractedRules.size} rules in total")
        }

        println("mining rules took ${miningTime.inWholeSeconds}s ")
        return extractedRules
    }

    // saves rules to file
    // TODO: replace prefixes with long IRIs; to read them later
    fun saveRulesToFile(rules: Set<AssociationRule>, file: File): Boolean {
        if (file.exists()) return false

        return try {
            FileWriter(file).use { fileWriter ->
                rules.forEach { r -> fileWriter.write("${r.minedString}\n") }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

