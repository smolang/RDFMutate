package domainSpecific.geo

import org.apache.jena.rdf.model.Model
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


// generates several scenarios
class GeoScenarioGenerator (){

    // list of files + oracle (does maturation happen)
    val files : MutableList<Pair<File,Boolean>> = mutableListOf()
    fun  generateScenarios(count : Int) {
        // generate scenario main blocks
        "python3 generateScenarios.py scenarios $count".runCommand(File("sut/geo"))

        // iterate over scenarios
        val dir = File("sut/geo/scenarios")
        val pattern = Regex("sut/geo/scenarios/Output.*.txt");
        val header = "sut/geo/scenarios/header.smol"

        var i = 0

        dir.walk().forEach {outputFile ->
            if (pattern.matches(outputFile.toString())) {
                println(outputFile)
                val scenarioName = "${dir.absolutePath}/scenario$i.smol"
                val scenario = File(scenarioName)
                scenario.createNewFile()

                var has_source = true
                var has_cap = true
                var depth = 0

                println(scenario.absolutePath)
                FileOutputStream(scenarioName).use { fos ->
                    val writer = fos.bufferedWriter()
                    writer.write("id;folder;mutantFile;numMutations;numDel;numAdd;appliedMutations;affectedSeedNodes;addedAxioms;removedAxioms")
                    writer.newLine()
                }
                scenario.createNewFile()
                File(scenarioName).printWriter().use { out ->
                    File(header).forEachLine { out.println(it) }
                    out.println("\n————————————————————————————————\n" +
                            "the following part is the generated scenario")
                    outputFile.forEachLine {
                        out.println(it)
                        if (it.startsWith(" has_source:"))
                            has_source =
                                if (it.endsWith("False"))
                                    false
                                else
                                    true
                        if (it.startsWith("has_cap:"))
                            has_cap =
                                if (it.endsWith("False"))
                                    false
                                else
                                    true
                        if (it.startsWith("depth:"))
                            depth = it.removePrefix("depth: ").toInt()
                    }
                }

                // delete file created by Python script
                outputFile.delete()

                // TODO: analyze if maturation happens and safe this accordingly
                val maturation = (has_source && depth > 2000)

                files.add(Pair(scenario, maturation))
                i += 1
            }
        }

        writeToCSV("${dir.absolutePath}/scenarios.csv")
    }


    // writes content from "files" list to csv
    fun writeToCSV(fileName : String) {
        FileOutputStream(fileName).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("id,scenario,maturation")
            writer.newLine()
            var id = 0
            for (f in files) {
                writer.write("$id,${f.first},${f.second}")
                writer.newLine()
                id += 1
            }
            writer.close()
        }
    }

        // from https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
    fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(600, TimeUnit.MINUTES)
    }

}