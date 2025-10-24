package org.smolang.robust.tools.extraction

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import java.io.File

class AssociationRuleExtractorTest : StringSpec(){
    init {
        "mine some operators" {
            val ontologyFiles = setOf(File("src/test/resources/ruleExtraction/ore_ont_155.owl"))
            val minRuleMatch = 50
            val minHeadMatch = 20
            val minConfidence = 0.8
            val maxRuleLength = 3

            val extractorBridge = ExtractorBridge(
                minRuleMatch,
                minHeadMatch,
                minConfidence,
                maxRuleLength)

            val rules = AssociationRuleExtractor().mineRules(extractorBridge, ontologyFiles)
            rules shouldNotBe null
            rules?.size shouldBe 8

            val operators = rules?.flatMap { it.getAbstractMutations() }
            operators?.size shouldBe 52

            //println(operators?.size)
        }
    }
}