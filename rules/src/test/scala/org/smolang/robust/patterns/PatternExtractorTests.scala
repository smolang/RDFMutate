package org.smolang.robust.patterns


import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith

import java.io.File
//import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PatternExtractorTests extends AnyFunSuite {
  test("mine rules from ore 155") {
    // pattern extractor for ORE ontologies
    val orePatternExtractor = new PatternExtractor(50, 20, 0.8, 3)
    val rules = orePatternExtractor.extractRules(new File("src/test/resources/ore_ont_155.owl"))
    assert(rules.size == 8)
  }
}