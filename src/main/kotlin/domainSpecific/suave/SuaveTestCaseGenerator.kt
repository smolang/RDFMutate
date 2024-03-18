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
    val suaveRulesPath = "sut/suave/suave_ontologies/suave_original_rules.owl"
    // start with loading the rules of the corresponding ontologies
    val mrosRulesModel = RDFDataMgr.loadDataset(mrosRulesPath).defaultModel
    //val mrosModel = ModelFactory.createDefaultModel()
    val tomasysRulesModel = RDFDataMgr.loadDataset(tomasysRulesPath).defaultModel
    val suaveRulesModel = RDFDataMgr.loadDataset(suaveRulesPath).defaultModel


    val mrosPath = "sut/suave/suave_ontologies/mros.owl"
    val tomasysPath = "sut/suave/suave_ontologies/tomasys.owl"
    val mrosModel = RDFDataMgr.loadDataset(mrosPath).defaultModel
    //val mrosModel = ModelFactory.createDefaultModel()
    val tomasysModel = RDFDataMgr.loadDataset(tomasysPath).defaultModel
    fun generateSuaveMutants(numberMutants : Int) {
        // general approach:
        // (1) load ontology but without SWRL rules (we do not want to mutate them)
        // (2) apply mutations
        // (3) distinguish between three ontologies, especially the first case is important!:
        // (3.a) suave
        // (3.b) mros
        // (3.c) thomasys
        // add rules back to the corresponding ontologies
        // safe all three ontologies separately

        // maybe fist hacky: only save suave stuff

        // load ontology without rules, we do not want to mutate them
        val pathSeed = "sut/suave/suave_ontologies/suave_original_with_imports_no_rules.owl"
        val seed = RDFDataMgr.loadDataset(pathSeed).defaultModel


        // empty contract
        val contract = MutantContract(verbose)
        val additionalAxioms = ModelFactory.createDefaultModel()
        for (s in tomasysRulesModel.listStatements())
            additionalAxioms.add(s)
        for (s in mrosRulesModel.listStatements())
            additionalAxioms.add(s)
        for (s in suaveRulesModel.listStatements())
            additionalAxioms.add(s)



        //val suaveGenerator = SuaveMutatorFactory(verbose, maxMutation)

        // create as many mutants as specified
        /*super.generateMutants(
            seed,
            contract,
            suaveGenerator,
            numberMutants
        )

         */

        val mutationNumbers = listOf<Int>(1)//,5,10)
        for (i in mutationNumbers) {
            val suaveGenerator = SuaveMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                contract,
                suaveGenerator,
                10
            )
        }

        saveMutants("sut/suave/mutatedOnt", "onlySuave01")
        super.writeToCSV("sut/suave/mutatedOnt/onlySuave01.csv")
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

            for (s in stats)
                suaveRulesModel.add(s)


            // three files for the three models
           // val mrosOutputPath = "$folderName/$filePrefix.$i.mros.owl"
            //val tomasysOutputPath = "$folderName/$filePrefix.$i.tomasys.owl"
            val suaveOutputPath = "$folderName/$filePrefix.$i.suave.owl"

            //RDFDataMgr.write(File(mrosOutputPath).outputStream(), mrosRulesModel, Lang.RDFXML)
            //RDFDataMgr.write(File(tomasysOutputPath).outputStream(), tomasysRulesModel, Lang.RDFXML)
            RDFDataMgr.write(File(suaveOutputPath).outputStream(), suaveRulesModel, Lang.RDFXML)

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





class SuaveMutatorFactory(verbose: Boolean, private val maxNumberMutations: Int) : MutatorFactory(verbose) {
    // defines, if all mutation sequences have the same (maximal) length
    val constantNumberOfMutations = true


    val ratioDomainDependent = 1.0

    private val domainSpecificMutations = listOf(
        ChangeSolvesFunctionMutation::class,
        AddQAEstimationMutation::class,
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
        ms.addRelevantPrefix("http://www.metacontrol.org/suave")

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