import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mutant.*
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFLanguages


class Main : CliktCommand() {
    private val source by argument().file()
    private val contract by argument().file()
    private val verbose by option("--verbose","-v", help="Verbose output for debugging. Default = false.").flag()
    private val rounds by option("--rounds","-r", help="Number of mutations applied to input. Default = 1.").int().default(1)

    override fun run() {

        if(!source.exists()) throw Exception("Input file $source does not exist")
        val input = RDFDataMgr.loadDataset(source.absolutePath).defaultModel
        if(!contract.exists()) throw Exception("Contract file $contract does not exist")
        val contractModel = RDFDataMgr.loadDataset(contract.absolutePath).defaultModel

        var n = 0
        while(true) {
            println("\n generation ${n++}")
            val m = Mutator(listOf(AddInstanceMutation::class, RemoveAxiomMutation::class), verbose)

            //this is copying before mutating, so we must not copy one more time here
            val res = m.mutate(input, rounds)

            //XXX: the following ignores blank nodes
            val valid = m.validate(res, contractModel)
            println("result of validation: $valid")
            if(valid) {
                if(verbose) res.write(System.out, "TTL")
                break
            }
        }
    }
}


fun main(args: Array<String>) = Main().main(args)

