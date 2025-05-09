import io.kotlintest.specs.StringSpec
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.domainSpecific.geo.GeoTestCaseGenerator
import org.smolang.robust.mutant.RobustnessMask
import java.io.File

class GeoSpecificTests() : StringSpec() {
    /*
    // do not use this --> calls external program, requiring valid python installation
    init {
        "generate geo scenarios" {
            val geoGenerator = GeoScenarioGenerator()
            geoGenerator.generateScenarios(count = 1, saveScenarios = false)
        }
    }
     */

    init {
        "generate geo test cases" {
            val maskFiles = listOf(
                "src/test/resources/geo/masks/mask0.ttl",
                "src/test/resources/geo/masks/mask1.ttl"
            )

            var id = 0
            for (maskFile in maskFiles) {
                val gg = GeoTestCaseGenerator()
                val numberOfMutants = 2
                val numberOfMutations = 2

                val shapesGraph = RDFDataMgr.loadGraph(File(maskFile).absolutePath)
                val shapes = Shapes.parse(shapesGraph)
                val mask = RobustnessMask(shapes)

                val nameOfMutants = "geoMutant$id"
                val saveMutants = false
                gg.generateGeoMutants(
                    numberOfMutants,
                    numberOfMutations,
                    mask,
                    nameOfMutants,
                    saveMutants
                )

                id += 1
            }
        }
    }

    init {
        "evaluate geo mask outputs" {
            val initialMaskFile = File("src/test/resources/geo/initialMask.ttl")
            val initialShapes = Shapes.parse(RDFDataMgr.loadGraph(initialMaskFile.absolutePath))
            val initialMask = RobustnessMask(initialShapes)

            val generatedMaskFile = File("src/test/resources/geo/generatedMask.ttl")
            val generatedShapes = Shapes.parse(RDFDataMgr.loadGraph(generatedMaskFile.absolutePath))
            val generatedMask = RobustnessMask(generatedShapes)

            val relevantSutRuns = listOf(
                "src/test/resources/geo/geo_oracles.csv"
            )
            val detailedEvaluationOutput = true

            initialMask.checkAgainstOntologies(
                relevantSutRuns,
                detailedEvaluationOutput)

            generatedMask.checkAgainstOntologies(
                relevantSutRuns,
                detailedEvaluationOutput)
        }
    }

}