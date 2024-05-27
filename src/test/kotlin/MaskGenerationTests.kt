import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.mutant.ShapeGenerator
import java.io.File

class MaskGenerationTests : StringSpec() {
    init {
        "read SUAVE axioms to be contained and check if original suave ontology complies" {

            val sg = ShapeGenerator()
            sg.turnAxiomsToShapes("src/test/resources/suave/contract7.owl")
            sg.saveShapes("src/test/resources/suave/", "generatedMask")

            // read the generated shapes and turn them into maks
            val shapesGraph = RDFDataMgr.loadGraph("src/test/resources/suave/generatedMask.ttl")
            val shapes = Shapes.parse(shapesGraph)
            val mask = RobustnessMask(false, shapes)

            val suavePath = "src/test/resources/suave/suave_original_with_imports.owl"
            val badMutantPath = "src/test/resources/suave/suave_bad.owl"
            val original = RDFDataMgr.loadDataset(suavePath).defaultModel
            val badMutant = RDFDataMgr.loadDataset(badMutantPath).defaultModel

            mask.validate(original) shouldBe true
            mask.validate(badMutant) shouldBe false
        }
    }

    init {
        "read geo axioms to be contained and check if original geo ontology complies" {

            val sg = ShapeGenerator()
            sg.turnAxiomsToShapes("src/test/resources/geo/contract1.ttl")
            sg.saveShapes("src/test/resources/geo/", "generatedMask")

            // read the generated shapes and turn them into maks
            val shapesGraph = RDFDataMgr.loadGraph("src/test/resources/geo/generatedMask.ttl")
            val shapes = Shapes.parse(shapesGraph)
            val mask = RobustnessMask(false, shapes)

            val originalPath = "src/test/resources/geo/geo_original.ttl"
            val badMutantPath = "src/test/resources/geo/geo_bad.nt"

            val original = RDFDataMgr.loadDataset(originalPath).defaultModel
            val badMutant = RDFDataMgr.loadDataset(badMutantPath).defaultModel

            mask.validate(original) shouldBe true
            mask.validate(badMutant) shouldBe false
        }
    }
}