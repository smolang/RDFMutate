package mutant

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

class MutatorList(private val verbose: Boolean) {
    var seed : Model? = null
    val mutators : MutableList<Mutator> = mutableListOf()
    val mutants : MutableList<Model> = mutableListOf()
    val mutantFiles: MutableList<String> = mutableListOf()
    var ran : Boolean = false

    fun addMutator(m : Mutator) {
        mutators.add(m)
        mutantFiles.add("?")    // path is not known yet
    }

    fun createMutants(seed : Model) {
        ran = true
        for (mutator in mutators)
            mutants.add(mutator.mutate(seed))
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
        assert(ran)
        FileOutputStream(fileName).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("id,mutantFile,numDel,numAdd,affectedSeedNodes")
            writer.newLine()
            var i = 0
            for (m in mutators) {
                val id = 0
                val mutantFile = mutantFiles[i]
                val numDel = m.globalMutation!!.deleteSet.size
                val numAdd = m.globalMutation!!.addSet.size
                val affectedSeedNodes = m.affectedSeedNodes.map { it.localName }.joinToString(" ", "[", "]")
                writer.write("$id,$mutantFile,$numDel,$numAdd,$affectedSeedNodes")
                i += 1
            }
            writer.close()
        }
    }
}