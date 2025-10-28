package org.smolang.robust.tools.extraction

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File

class ExtractionRunnerTest: StringSpec() {
    init {
        "simple rule mining / parsing of config" {
            val runner = ExtractionRunner(
                File("src/test/resources/ruleExtraction/ore_155_extraction.yaml")
            )
            val result1 = runner.extract()
            result1 shouldBe ExtractionOutcome.SUCCESS

        }
    }
}