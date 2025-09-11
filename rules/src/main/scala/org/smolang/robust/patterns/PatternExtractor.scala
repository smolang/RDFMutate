package org.smolang.robust.patterns

import com.github.owlcs.ontapi.OntManagers
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index.{IndexPart, TripleItemIndex}
import com.github.propi.rdfrules.rule.{Atom, Measure, Threshold}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.utils.Debugger.EmptyDebugger
import com.github.sszuev.jena.ontapi.model.OntModel
import org.apache.jena.assembler.RuleSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{Lang, RDFDataMgr}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileOutputStream}

class PatternExtractor(val minRuleMatch: Int,   // how often rule matches completely
                       val minHeadMatch: Int,   // how often head matches
                       val minConfidence: Double,    // ratio how often rule matches when body matches
                       val maxRuleLength: Int   // maximal length of rule (head + body)
                      ) {

    // extracts rules for the graph from a file
    def extractRules(graphFile: File): Unit = {
        val d = readGraphFile(graphFile)

        println("mine rules in graph " + graphFile.getName)
        println("triples in graph: " + d.size)
        println("extract rules; rule is at least " + minRuleMatch + " times satisfied")
        val rules = mineRules(d)
        val filteredRules = rules
          .computeConfidence(minConfidence)(Measure.CwaConfidence, EmptyDebugger)
          .sortBy(Measure.Support)

        // remove all rules that contain constants (IRIs)
        val generalRules = removeRulesWithConstants(filteredRules)

        println("mined " + generalRules.size + " rules from graph")

        // remove
        generalRules.foreach(println)
        //filteredRules.foreach(println)
    }


    private def mineRules(d: Dataset): Ruleset = {
        val miningTask = Amie()
        val restrictedMiningTask = miningTask
          .addThreshold(Threshold.MinSupport(minRuleMatch))    // rule needs to be satisfied minMatch often
          .addThreshold(Threshold.MinHeadSize(minHeadMatch))          // rule head needs to match at least 50 times
          .addThreshold(Threshold.MaxRuleLength(3))

        Debugger() { implicit debugger =>
            val index = d.index(debugger)
            // mine rules
            val rules = index.mineRules(restrictedMiningTask)
            return rules
        }
    }

    private def removeRulesWithConstants(rules: Ruleset) : Ruleset = {
        val map = rules.index.tripleItemMap
        rules.filter(r =>
            {
                if (hasConstant(r.head, map))
                    false
                else if (hasConstant(r.bodySet, map))
                    false
                else
                    true
            }

        )
    }

    private  def hasConstant(atoms: Set[Atom], map: TripleItemIndex): Boolean = {
        atoms.foreach(a =>
          if (hasConstant(a, map))
              return true
        )
        false
    }

    private def hasConstant(atom: Atom, map: TripleItemIndex): Boolean = {
        val s = atom.subject match {
            case constant: Atom.Constant => map.getTripleItem(constant.value).toString
            case _ => atom.subject.toString
        }
        val p = map.getTripleItem(atom.predicate).toString
        val o = atom.`object` match {
            case constant: Atom.Constant => map.getTripleItem(constant.value).toString
            case _ => atom.`object`.toString
        }

        //println(s + ", " + p + ", " + o)

        if (isConstant(s) || isConstant(p) || isConstant(o))
            true
        else
            false
    }

    private def isConstant(s: String): Boolean = {
        s.startsWith("<") && s.endsWith(">")
    }



    private def readGraphFile(inputFile: File): Dataset =  {
        if (inputFile.getPath.endsWith(".ttl"))
            Dataset(inputFile.getAbsolutePath)

        else if (inputFile.getPath.endsWith(".owl")) {
            val graph = readOWLFile(inputFile)
            val format = Lang.TTL

            // stream version does not work --> need to use temp file
            val tempFile = new File("temp.ttl")


            //val tempStream = new ByteArrayOutputStream()

            //RDFDataMgr.write(tempStream, graph, format)
            RDFDataMgr.write(new FileOutputStream(tempFile), graph, format)

            //val data = tempStream.toByteArray
            //val tempInputStream = new ByteArrayInputStream(data)
            Dataset(tempFile)(format)
        }
        else {
            // return empty dataset if we can not read file
            Dataset()
        }
    }

    private def readOWLFile(inputFile: File) : Model = {
        val manager = OntManagers.createManager()
        val ontology = manager.loadOntologyFromOntologyDocument(inputFile)
        return ontology.asGraphModel()
    }

}
