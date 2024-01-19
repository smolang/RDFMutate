package domainSpecific

import mutant.*
import org.apache.jena.riot.RDFDataMgr
import randomGenerator

class SuaveTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {

    fun generateSuaveMutants(numberMutants : Int) {
        val pathSeed = "sut/suave/suave_ontologies/suave_original_with_imports.owl"
        val seed = RDFDataMgr.loadDataset(pathSeed).defaultModel

        // maximal number of mutations to generate a mutant
        val maxMutation = 5

        // empty contract
        val contract = MutantContract(verbose)
        val suaveGenerator = SuaveMutatorFactory(verbose, maxMutation)

        // create as many mutants as specified
        super.generateMutants(
            seed,
            contract,
            suaveGenerator,
            numberMutants
        )



        //super.saveMutants("sut/suave/mutatedOnt", "firstMutations")
        //super.writeToCSV("sut/suave/mutatedOnt/overview.csv")
    }
}

class SuaveMutatorFactory(verbose: Boolean, private val maxNumberMutations: Int) : MutatorFactory(verbose) {
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

        // determine the number of the applied mutation operators
        val domSpecMut = randomGenerator.nextInt(0, maxNumberMutations+1)
        val domIndMut =
            if (domSpecMut == 0 && maxNumberMutations != 0)
                randomGenerator.nextInt(1, maxNumberMutations+1)
            else
                randomGenerator.nextInt(0, maxNumberMutations-domSpecMut+1)

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