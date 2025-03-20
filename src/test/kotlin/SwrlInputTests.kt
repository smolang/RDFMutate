import io.kotlintest.specs.StringSpec
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mutant.RuleParser

class SwrlInputTests : StringSpec() {

    init {
        "test parsing of simple SWRL rule" {
            val input = RDFDataMgr.loadDataset("swrl/swrlTest.ttl").defaultModel

            val parser = RuleParser(input)

            parser.getAllRuleMutations()
        }
    }
}