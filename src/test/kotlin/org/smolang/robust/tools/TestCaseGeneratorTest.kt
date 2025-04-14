package org.smolang.robust.tools

import io.kotlintest.specs.StringSpec
import org.apache.jena.riot.RDFDataMgr
import org.smolang.robust.mutant.EmptyRobustnessMask
import org.smolang.robust.mutant.MutatorFactory

class TestCaseGeneratorTest: StringSpec(){
    init {
        "Export of results and saving mutants" {
            val generator = TestCaseGenerator()
            generator.generateMutants(
                RDFDataMgr.loadDataset("abc/abc.ttl").defaultModel,
                EmptyRobustnessMask(),
                MutatorFactory(),
                countDesired = 5
            )

            generator.saveMutants(
                "src/test/resources/tempOutputs/",
                "generatorTest"
            )

            generator.writeToCSV("src/test/resources/tempOutputs/summary.csv")
        }
    }

}