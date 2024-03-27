package domainSpecific.geo

import mutant.*
import org.apache.jena.riot.RDFDataMgr
import randomGenerator

class GeoTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {

    val geoOntoPath = "sut/geo/total_mini.ttl"
    fun generateGeoMutants() {
        val seed = RDFDataMgr.loadDataset(geoOntoPath).defaultModel
        //for (s in seed.listStatements())
        //    println(s)

        val geoGenerator = GeoMutatorFactory(verbose, 1)
        val contract = MutantContract(verbose)

        val mutationNumbers = listOf<Int>(10)//,2,3,4,5,6,7,8,9,10)
        for (i in mutationNumbers) {
            val geoMutator = GeoMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                contract,
                geoMutator,
                1 //10
            )
        }

        saveMutants("sut/geo/mutatedOnt", "thirdTest")
        super.writeToCSV("sut/geo/mutatedOnt/thirdTest.csv")
    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {
    val ratioDomainDependent = 0.0

    private val domainIndependentMutations = listOf(
        CEUAMutation::class,
        ChangeDataPropertyMutation::class,  // test also, if we could replace with other datatype
        ChangeDoubleMutation::class,        // also targets T-Box
        ACATOMutation::class,
        ToSiblingClassMutation::class,
    )

    override fun randomMutator(): Mutator {
        val ms = MutationSequence(verbose)

        for (i in 1..NumberMutations)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        return Mutator(ms, verbose)
    }
}

