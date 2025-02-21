import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.domainSpecific.reasoner.OwlOntologyAnalyzer

class OntologyAnalyzerTests : StringSpec() {

    init {
        "test analyzing owl features" {
            val input1 = RDFDataMgr.loadDataset("reasoners/assertion.ttl").defaultModel
            OwlOntologyAnalyzer().getFeatures(input1).size shouldBe 4

            val input2 = RDFDataMgr.loadDataset("reasoners/siblings.ttl").defaultModel
            OwlOntologyAnalyzer().getFeatures(input2).size shouldBe 5

            val geo = RDFDataMgr.loadDataset("geo/geo_original.ttl").defaultModel
            OwlOntologyAnalyzer().getFeatures(geo).size shouldBe 49

        }
    }
}