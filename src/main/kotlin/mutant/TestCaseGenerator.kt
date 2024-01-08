package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

open class TestCaseGenerator(private val verbose: Boolean) {
    val mutators : MutableList<Mutator> = mutableListOf()
    val mutants : MutableList<Model> = mutableListOf()
    val mutantFiles: MutableList<String> = mutableListOf()

    fun generateMutants(seed : Model,
                        contract: MutantContract,
                        mutFactory : MutatorFactory,
                        countDesired : Int) {
        var countGenerated = 0
        while (countGenerated < countDesired) {
            val m = mutFactory.randomMutator()
            val mutant = m.mutate(seed)
            println(countGenerated)
            if (contract.validate(mutant)){
                countGenerated += 1
                mutators.add(m)
                mutants.add(mutant)
                mutantFiles.add("?")
            }
        }
    }



    fun saveMutants(folderName: String, filePrefix : String) {
        var i = 0
        // create folder, if necessary
        Files.createDirectories(Paths.get(folderName))
        for (mut in mutants) {
            val path = "$folderName/$filePrefix$i.ttl"
            RDFDataMgr.write(File(path).outputStream(), mut, Lang.TTL)
            mutantFiles[i] = path   // save path of the mutation
            i += 1
        }
    }

    fun writeToCSV(fileName : String) {
        FileOutputStream(fileName).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("id;mutantFile;numMutations;numDel;numAdd;appliedMutations;affectedSeedNodes;addedAxioms;removedAxioms")
            writer.newLine()
            var id = 0
            for (m in mutators) {
                val mutantFile = mutantFiles[id]
                val numMut = m.numMutations
                val numDel = m.globalMutation?.removeSet?.size ?: -1
                val numAdd = m.globalMutation?.addSet?.size ?: -1
                val appliedMutations = m.appliedMutations
                val affectedSeedNodes = m.affectedSeedNodes.map {
                    if (it.localName != null)
                        it.localName
                    else
                        it.toString()
                }.joinToString(",", "[", "]")

                val addedAxioms = m.addSet.joinToString( ",", "[", "]")
                val removedAxioms = m.removeSet.joinToString( ",", "[", "]").replace("\n", ",")

                writer.write("$id;$mutantFile;$numMut;$numDel;$numAdd;$appliedMutations;$affectedSeedNodes;$addedAxioms;$removedAxioms")
                writer.newLine()
                id += 1
            }
            writer.close()
        }
    }
}