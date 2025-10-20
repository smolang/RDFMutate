package org.smolang.robust.patterns

import com.github.propi.rdfrules.index.IndexCollections.MutableHashSet

import java.io.File
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object StartClass {

  private def exitWrongArguments(): Unit = {
    println("ERROR: invalid arguments")
    System.exit(2)  // indicate error
  }

  def callPatternExtractor(args: Array[String]): Unit = {
    // arguments: minRuleMatch, minHeadMatch, minConfidence, maxRuleLength, list of files
    if (args.length < 5)
      exitWrongArguments()

    var minRuleMatch = 0
    var minHeadMatch = 0
    var minConfidence = 0.0
    var maxRuleLength = 0

    // parse parameters
    try {
      minRuleMatch = args(0).toInt
      minHeadMatch = args(1).toInt
      minConfidence = args(2).toDouble
      maxRuleLength = args(3).toInt
    }
    catch {
      case _ : NumberFormatException => exitWrongArguments()
    }

    val ontologyFiles: mutable.HashSet[File]  = mutable.HashSet[File]()
    // parse file names
    for (i <- 4 until args.length) {
      ontologyFiles.add(new File(args(i)))
    }
    //val ontologyFile = new File("src/test/resources/ruleExtraction/ore_ont_155.owl")
    val extractor = new PatternExtractor(minRuleMatch, minHeadMatch, minConfidence, maxRuleLength)

    // call extractor
    val minedRules = extractor.extractRules(ontologyFiles.toSet.asJava)

    // print the mined rules
    println("RULES START")
    minedRules.foreach(println(_))
    println("RULES END")
  }

  def main(args: Array[String]): Unit = {
    callPatternExtractor(args)
  }
}
