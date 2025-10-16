import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.XSD
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.tools.ruleMutations.SWRLRuleParser
import java.io.File

class SwrlInputTests : StringSpec() {

    init {
        "test parsing an applying of simple SWRL rule" {
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTest.ttl").defaultModel

            val parser = SWRLRuleParser(File("src/test/resources/swrl/swrlTest.ttl"))
            val ruleMutations = parser.getAllAbstractMutations()

            ruleMutations!!.size shouldBe 1

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
            )) shouldBe true

        }
    }

    init {
        "parse mutation from SWRL rule with general property" {
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTestArbitratyRelation.ttl").defaultModel

            val parser = SWRLRuleParser(File("src/test/resources/swrl/swrlTestArbitratyRelation.ttl"))
            val ruleMutations = parser.getAllAbstractMutations()

            ruleMutations!!.size shouldBe 1

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

            val parser = SWRLRuleParser(File("src/test/resources/swrl/swrlTestComplex.ttl"))
            val ruleMutations = parser.getAllAbstractMutations()

            ruleMutations!!.size shouldBe 3
        }
    }

    init {
        "test applying rules that refer to data relations" {
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTest.ttl").defaultModel

            // parse the two mutations from swrl rules
            val parser1 = SWRLRuleParser(File("src/test/resources/swrl/swrlTestDp1.ttl"))
            val ruleMutation1 = parser1.getAllAbstractMutations()!!.single()
            val parser2 = SWRLRuleParser(File("src/test/resources/swrl/swrlTestDp2.ttl"))
            val ruleMutation2 = parser2.getAllAbstractMutations()!!.single()

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
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTestNegatedClass.ttl").defaultModel

            val mutation = SWRLRuleParser(File("src/test/resources/swrl/swrlTestNegatedClass.ttl")).getAllAbstractMutations()!!.single()

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

    init {
        "SWRL rule with negative property assertion" {
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTestNegatedProperty.ttl").defaultModel

            val mutation = SWRLRuleParser(File("src/test/resources/swrl/swrlTestNegatedProperty.ttl")).getAllAbstractMutations()!!.single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(input)

            result.contains(
                input.createStatement(
                    input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b"),
                    RDF.type,
                    input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#B")
                )
            ) shouldBe true

        }
    }

    init {
        "SWRL rule with negative consequences" {
            val input = RDFDataMgr.loadDataset("src/test/resources/swrl/swrlTestNegatedConsequence.ttl").defaultModel

            val mutation = SWRLRuleParser(File("src/test/resources/swrl/swrlTestNegatedConsequence.ttl")).getAllAbstractMutations()!!.single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(input)


            val testStatement = input.createStatement(
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#a"),
                RDF.type,
                input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#A")
            )

            input.contains(testStatement ) shouldBe true
            result.contains(testStatement ) shouldBe false

            input.listStatements().toSet().size -1 shouldBe result.listStatements().toSet().size

        }
    }

    init {
        "SWRL with new node declaration" {
            val seed = RDFDataMgr.loadDataset("src/test/resources/PipeInspection/miniPipes.ttl").defaultModel
            val mutation = SWRLRuleParser(File("src/test/resources/PipeInspection/addPipeSegment.ttl")).getAllAbstractMutations()!!.single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(seed)

            // check, that there is one more segment individual
            result.listStatements(
                null,
                RDF.type,
                result.getResource("http://www.ifi.uio.no/tobiajoh/miniPipes#PipeSegment")
            ).toSet().size shouldBe 3

        }
    }

    init {
        "SWRL with node deletion" {
            val seed = RDFDataMgr.loadDataset("src/test/resources/PipeInspection/miniPipes.ttl").defaultModel
            val mutation = SWRLRuleParser(File("src/test/resources/PipeInspection/removePipeSegment.ttl")).getAllAbstractMutations()!!.single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(seed)

            // check, that there is one segment individual
            result.listStatements(
                null,
                RDF.type,
                result.getResource("http://www.ifi.uio.no/tobiajoh/miniPipes#PipeSegment")
            ).toSet().size shouldBe 1

        }
    }

    init {
        "SWRL with node replacement + empty body" {
            val seed = RDFDataMgr.loadDataset("src/test/resources/PipeInspection/miniPipes.ttl").defaultModel
            val mutation = SWRLRuleParser(File("src/test/resources/PipeInspection/replacePipeSegment2.ttl")).getAllAbstractMutations()!!.single()

            // apply mutations
            val ms = MutationSequence()
            ms.addAbstractMutation(mutation)
            val mutator = Mutator(ms)
            val result = mutator.mutate(seed)

            // check, that there are two segment individuals
            result.listStatements(
                null,
                RDF.type,
                result.getResource("http://www.ifi.uio.no/tobiajoh/miniPipes#PipeSegment")
            ).toSet().size shouldBe 2

            val segment1 = result.getResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            val newSegment = result.getResource("http://www.ifi.uio.no/tobiajoh/miniPipes#newSegment")
            // check, that for each relation in seed with "segment1"  as subject, there is a relation with newSegment in mutant
            seed.listStatements(
                segment1,
                null,
                null as RDFNode?
            ).forEach { statement ->
                result.listStatements(
                    newSegment,
                    statement.predicate,
                    statement.`object`
                ).hasNext() shouldBe true
            }

            // check, that for each relation in seed with "segment1"  as object, there is a relation with newSegment in mutant
            seed.listStatements(
                null,
                null,
                segment1
            ).forEach { statement ->
                result.listStatements(
                    statement.subject,
                    statement.predicate,
                    newSegment
                ).hasNext() shouldBe true
            }
        }
    }
}