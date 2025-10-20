package org.smolang.robust.patterns

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith

import java.io.File
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StartClassTests extends AnyFunSuite {
  test("test valid input") {
    // pattern extractor for ORE ontologies
    val input = List("50", "20", "0.8", "3", "src/test/resources/ore_ont_155.owl")
    StartClass.callPatternExtractor(input.toArray) // should not lead to any exception
  }
}