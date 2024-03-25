package domainSpecific.geo

import domainSpecific.suave.SuaveMutatorFactory
import mutant.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Property
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

        val mutationNumbers = listOf<Int>(1,2,3,4,5,6,7,8,9,10)
        for (i in mutationNumbers) {
            val geoMutator = GeoMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                contract,
                geoMutator,
                10
            )
        }

        saveMutants("sut/geo/mutatedOnt", "secondTest")
        super.writeToCSV("sut/geo/mutatedOnt/secondTest.csv")
    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {
    val ratioDomainDependent = 0.0

    private val domainIndependentMutations = listOf(
        CEUAMutation::class,
        ChangeDataPropertyMutation::class,
        ACATOMutation::class,
        ToSiblingClassMutation::class
    )

    override fun randomMutator(): Mutator {
        val ms = MutationSequence(verbose)

        for (i in 1..NumberMutations)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        return Mutator(ms, verbose)
    }
}

