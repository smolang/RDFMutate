package domainSpecific.suave

import mutant.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import randomGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class SuaveTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {
    // maximal number of mutations to generate a mutant
    var maxMutation = 5

    val mrosRulesPath = "sut/suave/suave_ontologies/mros_rules.owl"
    val tomasysRulesPath = "sut/suave/suave_ontologies/tomasys_rules.owl"
    //val suaveRulesPath = "sut/suave/suave_ontologies/suave_original_rules.owl"
    // start with loading the rules of the corresponding ontologies
    val mrosRulesModel = RDFDataMgr.loadDataset(mrosRulesPath).defaultModel
    //val mrosModel = ModelFactory.createDefaultModel()
    val tomasysRulesModel = RDFDataMgr.loadDataset(tomasysRulesPath).defaultModel
    //val suaveRulesModel = RDFDataMgr.loadDataset(suaveRulesPath).defaultModel

    // load ontology without rules, we do not want to mutate them
    val mutatableSeedPath = "sut/suave/suave_ontologies/suave_mutatable.owl"
    val unmutatableSeedPath = "sut/suave/suave_ontologies/suave_unmutatable.owl"
    val mutatableStatements = RDFDataMgr.loadDataset(mutatableSeedPath).defaultModel.listStatements().toList()
    val unmutatableSuaveStatements = RDFDataMgr.loadDataset(unmutatableSeedPath).defaultModel.listStatements().toList()


    val mrosPath = "sut/suave/suave_ontologies/mros_no_import.owl"
    val tomasysPath = "sut/suave/suave_ontologies/tomasys.owl"
    val mrosModel = RDFDataMgr.loadDataset(mrosPath).defaultModel
    //val mrosModel = ModelFactory.createDefaultModel()
    val tomasysModel = RDFDataMgr.loadDataset(tomasysPath).defaultModel

    fun generateSuaveMutants(numberMutants : Int, contractFile : String) {
        val unmutatableStatements = unmutatableSuaveStatements.toMutableList()


        // empty contract
        val contract = MutantContract(verbose)
        contract.containedModel = RDFDataMgr.loadDataset(contractFile).defaultModel

        val ruleAxioms = ModelFactory.createDefaultModel()
      /*  for (s in tomasysRulesModel.listStatements())
            ruleAxioms.add(s)
        for (s in mrosRulesModel.listStatements())
            ruleAxioms.add(s)
        for (s in suaveRulesModel.listStatements())
            ruleAxioms.add(s)

        // contract with additional information to consider
        contract.additionalAxioms = ruleAxioms

       */

        // add more statements to unmutatable part
        for (s in tomasysModel.listStatements())
            unmutatableStatements.add(s)
        for (s in mrosModel.listStatements())
            unmutatableStatements.add(s)


        // compute complete seed with all axioms
        val seed = ModelFactory.createDefaultModel()
        for (s in unmutatableStatements)
            seed.add(s)
        for (s in mutatableStatements)
            seed.add(s)

        //val suaveGenerator = SuaveMutatorFactory(verbose, maxMutation)

        // create as many mutants as specified
        /*super.generateMutants(
            seed,
            contract,
            suaveGenerator,
            numberMutants
        )

         */

        val mutationNumbers = listOf<Int>(2)//3,4)
        for (i in mutationNumbers) {
            val suaveMutator = SuaveMutatorFactory(verbose, mutatableStatements, i)
            super.generateMutants(
                seed,
                contract,
                suaveMutator,
                30
            )
        }

        saveMutants("sut/suave/mutatedOnt", "temp")
        super.writeToCSV("sut/suave/mutatedOnt/temp.csv")
    }


     override fun saveMutants(folderName: String, filePrefix : String) {
        var i = 0


        // create folder, if necessary
        Files.createDirectories(Paths.get(folderName))
        for (mut in mutants) {
            // split the ontology into three parts, roughly based on the initial way:
            // if it contains suave prefix --> suave ontology
            // if it contains tomasys prefix --> tomasys ontology
            // else --> mros ontology
           /* for (stat in mut.listStatements()){
                if (containsPrefix(stat, "http://www.metacontrol.org/suave"))
                    suaveRulesModel.add(stat)
                else if (containsPrefix(stat, "http://ros/mros"))
                    mrosRulesModel.add(stat)
                else
                    tomasysRulesModel.add(stat)
            }

            */

            // find all elements in mutant that are not part of the original tomasys or mros ontology
            // these will be added to the rules of the suave ontology and will be saved in the end
            var stats = mut.listStatements().toSet()
            stats = stats.subtract(mrosModel.listStatements().toSet())
            stats = stats.subtract(tomasysModel.listStatements().toSet())

            val suaveOutput = ModelFactory.createDefaultModel()
            for (s in unmutatableSuaveStatements)
                suaveOutput.add(s)
            for (s in stats)
                suaveOutput.add(s)


            // three files for the three models
           // val mrosOutputPath = "$folderName/$filePrefix.$i.mros.owl"
            //val tomasysOutputPath = "$folderName/$filePrefix.$i.tomasys.owl"
            val suaveOutputPath = "$folderName/$filePrefix.$i.suave.owl"

            //RDFDataMgr.write(File(mrosOutputPath).outputStream(), mrosRulesModel, Lang.RDFXML)
            //RDFDataMgr.write(File(tomasysOutputPath).outputStream(), tomasysRulesModel, Lang.RDFXML)
            RDFDataMgr.write(File(suaveOutputPath).outputStream(), suaveOutput, Lang.RDFXML)

            mutantFiles[i] = suaveOutputPath   // save path of the mutation
            i += 1
        }
    }

    private  fun containsPrefix (stat: Statement, prefix : String) : Boolean {
        if (stat.subject.toString().contains(prefix))
            return true
        if (stat.predicate.toString().contains(prefix))
            return true
        if (stat.`object`.toString().contains(prefix))
            return true

        return false
    }
}





class SuaveMutatorFactory(
    verbose: Boolean,
    private val mutatableStatements: List<Statement>,
    private val maxNumberMutations: Int) : MutatorFactory(verbose) {

    // defines, if all mutation sequences have the same (maximal) length
    val constantNumberOfMutations = true


    private val ratioDomainDependent = 1.0

    private val domainSpecificMutations = listOf(
        ChangeSolvesFunctionMutation::class,
        //AddQAEstimationMutation::class,
        RemoveQAEstimationMutation::class,
        ChangeQualityAttributTypeMutation::class,
        ChangeHasValueMutation::class,
        ChangeQAComparisonOperatorMutation::class,
        AddNewThrusterMutation::class
    )

    private val domainIndependentMutations = listOf(
        AddRelationMutation::class,
        ChangeRelationMutation::class,
        AddInstanceMutation::class,
        RemoveAxiomMutation::class,
        RemoveNodeMutation::class
    )
    override fun randomMutator() : Mutator {

        val ms = MutationSequence(verbose)
        ms.addMutatableAxioms(mutatableStatements)

        val count =
            if (constantNumberOfMutations)
                maxNumberMutations
            else
                randomGenerator.nextInt(1, maxNumberMutations)

        // determine the number of the applied mutation operators
        val domSpecMut =
            (count*ratioDomainDependent).toInt()

        val domIndMut =
            count - domSpecMut


        // add domain specific mutations
        for (j in 1..domSpecMut)
            ms.addRandom(domainSpecificMutations.random(randomGenerator))

        // add domain independent mutation operators
        for (j in 1..domIndMut)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        // add mutations in random order to mutation sequence
        ms.shuffle()

        return Mutator(ms, verbose)
    }
}