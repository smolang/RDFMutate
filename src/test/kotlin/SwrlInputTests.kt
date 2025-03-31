import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.tools.RuleParser

class SwrlInputTests : StringSpec() {

    init {
        "test parsing an applying of simple SWRL rule" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTest.ttl").defaultModel

            val parser = RuleParser(input)
            val ruleMutations = parser.getAllRuleMutations()

            ruleMutations.size shouldBe 1

            val ms = MutationSequence()
            ms.addAbstractMutation(ruleMutations.single())

            val mutator = Mutator(ms)
            val result = mutator.mutate(input)

            val pRelations = result.listStatements(
                null,
                input.getProperty("http://www.ifi.uio.no/tobiajoh/swrlTest#p"),
                null as RDFNode?
            ).toSet()

            pRelations.size shouldBe 2

            pRelations.contains(input.createStatement(
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b"),
                input.getProperty("http://www.ifi.uio.no/tobiajoh/swrlTest#p"),
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#c")
            ))

        }
    }

    init {
        "parse mutation from SWRL rule with general property" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTestArbitratyRelation.ttl").defaultModel

            val parser = RuleParser(input)
            val ruleMutations = parser.getAllRuleMutations()

            ruleMutations.size shouldBe 1

            val ms = MutationSequence()
            ms.addAbstractMutation(ruleMutations.single())

            val mutator = Mutator(ms)
            val result = mutator.mutate(input)

            result.listStatements(
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b"),
                RDF.type,
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#A")
            ).toSet().size shouldBe 1

            result.listStatements().toSet().size shouldBe input.listStatements().toSet().size + 1


        }
    }

    init {
        "test parsing of complex SWRL rules" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTestComplex.ttl").defaultModel

            val parser = RuleParser(input)
            val ruleMutations = parser.getAllRuleMutations()

            ruleMutations.size shouldBe 3
        }
    }

    init {
        "test applying rules that refer to data relations" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTest.ttl").defaultModel

            // parse the two mutations from swrl rules
            val ruleFile1 = RDFDataMgr.loadDataset("swrl/swrlTestDp1.ttl").defaultModel
            val ruleFile2 = RDFDataMgr.loadDataset("swrl/swrlTestDp2.ttl").defaultModel
            val parser1 = RuleParser(ruleFile1)
            val parser2 = RuleParser(ruleFile2)
            val ruleMutation1 = parser1.getAllRuleMutations().single()
            val ruleMutation2 = parser2.getAllRuleMutations().single()

            // apply mutations
            val ms1 = MutationSequence()
            ms1.addAbstractMutation(ruleMutation1)
            val mutator1 = Mutator(ms1)
            val result1 = mutator1.mutate(input)

            val ms2 = MutationSequence()
            ms2.addAbstractMutation(ruleMutation2)
            val mutator2 = Mutator(ms2)
            val result2 = mutator2.mutate(input)

            result1.listStatements(
                null,
                null,
                input.createTypedLiteral("a new value", XSD.xstring.toString())
            ).toSet().size shouldBe 1

            result2.listStatements(
                null,
                RDF.type,
                input.createResource("http://www.ifi.uio.no/tobiajoh/swrlTest#HasDataValue")
            ).toSet().size shouldBe 1

            result1.listStatements().toSet().size shouldBe input.listStatements().toSet().size + 2
            result2.listStatements().toSet().size shouldBe input.listStatements().toSet().size + 1
        }
    }

    init {
        "SWRL rule with negative class assertion" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTestNegatedClass.ttl").defaultModel

            val mutation = RuleParser(input).getAllRuleMutations().single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(input)

            result.contains(
                input.createStatement(
                    input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b"),
                    RDF.type,
                    input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#A")
                )
            ) shouldBe true

        }
    }
}