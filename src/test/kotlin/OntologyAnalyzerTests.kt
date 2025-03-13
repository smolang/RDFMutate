import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.domainSpecific.reasoner.OntologyAnalyzer

class OntologyAnalyzerTests : StringSpec() {

    init {
        "test analyzing owl features" {
            val input1 = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel
            OntologyAnalyzer().getOwlFeatures(input1).size shouldBe 4

            val input2 = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel
            OntologyAnalyzer().getOwlFeatures(input2).size shouldBe 5

            val geo = RDFDataMgr.loadDataset("geo/geo_original.ttl").defaultModel
            OntologyAnalyzer().getOwlFeatures(geo).size shouldBe 49

        }
    }
}