import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.tools.RuleParser

class SwrlInputTests : StringSpec() {

    init {
        "test parsing of simple SWRL rule" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTest.ttl").defaultModel

            val parser = RuleParser(input)
            val ruleMutations = parser.getAllRuleMutations()

            ruleMutations.size shouldBe 1

            val ms = MutationSequence()
            ms.addAbstractMutation(ruleMutations.single())

            val mutator = Mutator(ms)
            val result = mutator.mutate(input)

            val pRelation = result.listStatements(
                null,
                input.getProperty("http://www.ifi.uio.no/tobiajoh/swrlTest#p"),
                null as RDFNode?
            ).toSet().single()

            println(pRelation)

            pRelation.subject shouldBe input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#b")
            pRelation.`object` shouldBe input.getResource("http://www.ifi.uio.no/tobiajoh/swrlTest#c")



        }
    }
}