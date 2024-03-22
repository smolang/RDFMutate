package domainSpecific.geo

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
        for (s in seed.listStatements())
            println(s)

        val geoGenerator = GeoMutatorFactory(verbose, 1)
        val contract = MutantContract(verbose)

        super.generateMutants(
            seed,
            contract,
            geoGenerator,
            1
        )

        saveMutants("sut/geo/mutatedOnt", "geoMutant1")

    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {
    val ratioDomainDependent = 0.0

    private val domainIndependentMutations = listOf(
        CEUAMutation::class,
    )

    override fun randomMutator(): Mutator {
        val ms = MutationSequence(verbose)

        ms.addRandom(domainIndependentMutations.random(randomGenerator))

        return Mutator(ms, verbose)
    }
}