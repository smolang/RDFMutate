package org.smolang.robust.patterns

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index.IndexPart
import com.github.propi.rdfrules.rule.{Measure, Threshold}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.Debugger.EmptyDebugger
import org.apache.jena.assembler.RuleSet

import java.io.File

class PatternExtractor(val minRuleMatch: Int,   // how often rule matches completely
                       val minHeadMatch: Int,   // how often head matches
                       val minConfidence: Double    // ratio how often rule matches when body matches
                      ) {

    // extracts rules for the graph from a file
    def extractRules(graphFile: File): Unit = {
        val d = Dataset(graphFile.getAbsolutePath)
        println("mine rules in graph " + graphFile.getName)
        println("triples in graph: " + d.size)
        println("extract rules; rule is at least " + minRuleMatch + " times satisfied")
        val rules = mineRules(d)
        val filteredRules = rules
          .computeConfidence(minConfidence)(Measure.CwaConfidence, EmptyDebugger)
          .sortBy(Measure.Support)

        println("mined " + filteredRules.size + " rules from graph")
        filteredRules.foreach(println)
    }

    private def mineRules(d: Dataset): Ruleset = {
        val miningTask = Amie()
        Debugger() { implicit debugger =>
            val index = d.index(debugger)
            val restrictedMiningTask = miningTask
              .addThreshold(Threshold.MinSupport(minRuleMatch))    // rule needs to be satisfied minMatch often
              .addThreshold(Threshold.MinHeadSize(minHeadMatch))          // rule head needs to match at least 50 times
            // mine rules
            val rules = index.mineRules(restrictedMiningTask)
            return rules
        }
    }
}
