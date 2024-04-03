package mutant

import mutant.reasoning.CustomReasonerFactory
import mutant.reasoning.ReasoningBackend
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.StmtIterator
import org.apache.jena.riot.RDFDataMgr
import java.io.File


class MutantContract(val verbose: Boolean) {
    var entailedModel : Model = ModelFactory.createDefaultModel()
    var containedModel : Model = ModelFactory.createDefaultModel()

    // additional axioms that are added to the ontology before reasoning, e.g. containing axioms not considered while
    // creating the mutations
    var additionalAxioms: Model = ModelFactory.createDefaultModel()

    // default: use Openllet for reasoning
    var reasoningBackend : ReasoningBackend = ReasoningBackend.OPENLLET



    // checks, if the provided model is valid w.r.t. the contract
    fun validate(model: Model) : Boolean {

        for (a in additionalAxioms.listStatements()) {
            model.add(a)
        }

        // create reasoner with the selected backend
        val reasonerFactory = CustomReasonerFactory(verbose)
        val reasoner = reasonerFactory.getReasoner(model, reasoningBackend)

        val consistent = reasoner.isConsistent()
        // always use JENA-API for containment check
        val containment = model.containsAll(containedModel)
        val entailment = reasoner.entailsAll(entailedModel)
        //println("$consistent, $containment,  $entailment")

        //val m = containedModel.listStatements().toSet()
        //m.removeAll(model.listStatements().toSet())
        //println(m)

        return  consistent
                && containment
                && entailment
    }

    // makes a prediction based on the contract
    fun contractOracle(ontologyPath: String): OracleOutcome {
        // build model for ontology
        val model = RDFDataMgr.loadDataset(ontologyPath).defaultModel

        if (this.validate(model))
            return OracleOutcome.PASS
        else
            return OracleOutcome.FAIL
    }

    // checks, if the predictions of the contract are in line with the predictions from the real oracle
    fun checkAgainstOntologies(oracleFiles: List<String>, detailedEvaluation: Boolean) {


        // sets to collect the ontologies for which the oracle is correct or wrong
        val correctOracle : MutableSet<String> = HashSet()
        val falsePass : MutableSet<String> = HashSet()
        val falseFail : MutableSet<String> = HashSet()

        for (oracleFilePath in oracleFiles) {
            // iterate through oracle file
            val file = File(oracleFilePath)
            file.forEachLine { line ->
                val tokens = line.split(",")
                assert(tokens.size == 4)
                val id = tokens[0]
                val folder = tokens[1]
                val ontologyPath = tokens[2]
                val oracle = tokens[3]
                if (id == "id") {
                    // are in first row
                    assert(folder == "folder") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                    assert(ontologyPath == "ontology") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                    assert(oracle == "oracle") { "Header of CSV file malformed. Should be \"id,folder,ontology,oracle\"" }
                } else {
                    // data row

                    // call contract oracle
                    val contractOracle = this.contractOracle(ontologyPath)

                    // check against real oracle
                    val realOracle = parseOutcome(oracle)

                    // collect deviations (wrong contract fail / pass)
                    if (realOracle != OracleOutcome.UNDECIDED) {// only consider cases where we have an oracle
                        if (contractOracle == realOracle)
                            correctOracle += ontologyPath
                        else
                            when (contractOracle) {
                                OracleOutcome.FAIL -> falseFail += ontologyPath
                                OracleOutcome.PASS -> falsePass += ontologyPath
                                OracleOutcome.UNDECIDED -> Unit
                            }
                    }
                }

            }
        }

        // print details of evaluation, if desired
        if (detailedEvaluation) {
            if (falseFail.any()) {
                println()
                println("false \"fail\" oracle from contract")
                for (ontology in falseFail)
                    println(ontology)
            }

            if (falsePass.any()) {
                println()
                println("false \"pass\" oracle from contract")
                for (ontology in falsePass.sorted())
                    println(ontology)
            }
        }

        // output result: is contract too permissive or too strict?
        println()
        println("———————————————————————————————————————")
        println("evaluation of contract:")
        println("correct: ${correctOracle.size}")
        println("falsePass: ${falsePass.size}")
        println("falseFail: ${falseFail.size}")
        if ( falsePass.size == 0 && falseFail.size >0 )
            println("The contract is too strict.")
        if ( falsePass.size > 0 && falseFail.size == 0 )
            println("The contract is too permissive.")
        if ( falsePass.size == 0 && falseFail.size == 0 )
            println("The contract correct w.r.t. the test cases.")


    }

    fun parseOutcome(outcome: String) :OracleOutcome {
        return when (outcome) {
            "passed" -> OracleOutcome.PASS
            "failed" -> OracleOutcome.FAIL
            "undecided" -> OracleOutcome.UNDECIDED
            else -> {
                assert(false) {"Argument $outcome can not be parsed to OracleOutcome."}
                OracleOutcome.PASS
            }
        }
    }
}

enum class OracleOutcome {
    PASS, FAIL, UNDECIDED;



}

