package org.smolang.robust.tools.extraction

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import java.io.File

class ExtractorBridgeTest : StringSpec() {
    init {
        "simple, passing test" {
            val ontologyFiles = setOf(File("src/test/resources/ruleExtraction/ore_ont_155.owl"))
            val minRuleMatch = 50
            val minHeadMatch = 20
            val minConfidence = 0.8
            val maxRuleLength = 3
            val extractor = ExtractorBridge( minRuleMatch,
                minHeadMatch,
                minConfidence,
                maxRuleLength)
            val rules = extractor.extractRules(ontologyFiles)
            rules shouldNotBe null
            rules?.size shouldBe 8
        }
    }


    init {
        "empty set of files -> should fail" {
            val ontologyFiles: Set<File> = setOf()
            val minRuleMatch = 50
            val minHeadMatch = 20
            val minConfidence = 0.8
            val maxRuleLength = 3
            val extractor = ExtractorBridge(minRuleMatch,
                minHeadMatch,
                minConfidence,
                maxRuleLength)
            val rules = extractor.extractRules(ontologyFiles)
            rules shouldNotBe null
            rules!!.isEmpty() shouldBe true
        }
    }


}