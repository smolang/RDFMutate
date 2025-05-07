package org.smolang.robust.domainSpecific.reasoner

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ElGenerationTimeAnalyzerTest : StringSpec() {
    init {
        "test time evaluation" {
            val sampleSize = 1
            val mutationCounts = listOf(1,10,100)
            val input = File("src/test/resources/reasoners/ore_ont_155.owl")
            val output = File("src/test/resources/tempOutputs/timeData.csv")

            // create folder if it does not exist
            output.parentFile.mkdirs()
            if (output.exists())
                output.delete()     // delete output file to get real result
            val timeout = 10000L // time in ms --> 10s
            ElGenerationTimeAnalyzer().timePerMutation(input, output, sampleSize, mutationCounts, timeout)

            // check if saved file is as expected
            val lines = output.readLines()
            lines.size shouldBe 4
            lines[0] shouldBe "seedSize,numMutations,computationTime"
            val time1 = lines[1].split(",")[2]
            val time100 = lines[3].split(",")[2]
            val mut100 = lines[3].split(",")[1]
            val seedSize10 = lines[2].split(",")[0]

            (time1.toInt() <= time100.toInt()) shouldBe true    // time for 100 mutations is larger than for 1
            mut100 shouldBe "100"
            seedSize10 shouldBe "2132"
        }
    }
}