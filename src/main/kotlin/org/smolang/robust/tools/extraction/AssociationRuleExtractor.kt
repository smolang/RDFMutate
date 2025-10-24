package org.smolang.robust.tools.extraction

import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.XSD
import org.eclipse.rdf4j.model.ModelFactory
import org.smolang.robust.mainLogger
import org.smolang.robust.tools.OwlOntologyInterface
import org.smolang.robust.tools.getJenaModel
//import org.smolang.robust.patterns.PatternExtractor
import java.io.File
import java.io.FileWriter
import kotlin.time.measureTime

// orchestrates the extraction of association rules from ontology files
class AssociationRuleExtractor {

    // extracts association rules from ontology files using an extractor bridge
    fun mineRules(
        extractorBridge: ExtractorBridge,
        inputFiles: Set<File>
    ) : Set<AssociationRule>? {
        val extractedRules: MutableSet<AssociationRule> = mutableSetOf()
        val miningTime = measureTime {

            // extract prefix map from files
            val prefixMap: MutableMap<String, String> = mutableMapOf()
            mainLogger.info("Extracting prefixes from ontology files as part of rule extraction process.")
            val prefixMaps = inputFiles.map { ontologyFile ->
                // load file (load owl f
                val model = ontologyFile.getJenaModel()
                model.nsPrefixMap.forEach { (prefix, iri) ->
                    if (prefixMap.containsKey(prefix) && prefixMap[prefix] != iri)
                        mainLogger.warn("The imported ontology files define prefix $prefix differently." +
                                "The IRI ${prefixMap[prefix]} will be used.")
                    else // we set new mapping
                        prefixMap[prefix] = iri
                }
            }
            mainLogger.info("Extracting prefixes from files completed. Found ${prefixMap.keys.size} prefix declarations.")

            val associationRuleFactory = AssociationRuleFactory(prefixMap)
            val rules = extractorBridge.extractRules(inputFiles)

            if (extractorBridge.status != ExtractorStatus.SUCCESS || rules == null) {
                mainLogger.error("Could not extract rules from knowledge graphs.")
                return null
            }

            rules.forEach { s ->
                // add association rule representing mined string rule
                extractedRules.add(
                    associationRuleFactory.getAssociationRule(s)
                )
            }
            mainLogger.info("mined ${extractedRules.size} rules in total")
        }

        mainLogger.info("mining rules took ${miningTime.inWholeSeconds}s ")
        return extractedRules
    }
}

