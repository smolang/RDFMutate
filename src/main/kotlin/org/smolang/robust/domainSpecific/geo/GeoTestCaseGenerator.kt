package org.smolang.robust.domainSpecific.geo

import org.smolang.robust.mutant.*
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.randomGenerator

class GeoTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {

    private val geoOntoPath = "org/smolang/robust/sut/geo/total_mini.ttl"
    fun generateGeoMutants(numberMutants : Int,
                           numberOfMutations : Int,
                           contractFile : String,
                           shapes: Shapes?,
                           mutantsName: String) {
        val seed = RDFDataMgr.loadDataset(geoOntoPath).defaultModel

        // new contract
        val contract = RobustnessMask(verbose, shapes, RDFDataMgr.loadDataset(contractFile).defaultModel, useReasonerContainment=true)

        val mutationNumbers = listOf(numberOfMutations)
        for (i in mutationNumbers) {
            val geoMutator = GeoMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                contract,
                geoMutator,
                numberMutants
            )
        }

        saveMutants("org/smolang/robust/sut/geo/mutatedOnt", mutantsName)
        super.writeToCSV("org/smolang/robust/sut/geo/mutatedOnt/"+mutantsName +".csv")
    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {

    private val domainIndependentMutations = listOf(
        CEUAMutation::class,
        CEUOMutation::class,
        ChangeDataPropertyMutation::class,  // test also, if we could replace with other datatype
        ChangeDoubleMutation::class,        // also targets T-Box
        ACATOMutation::class,
        ACOTAMutation::class,
        ToSiblingClassMutation::class,
    )

    override fun randomMutator(): Mutator {
        val ms = MutationSequence(verbose)

        for (i in 1..NumberMutations)
            ms.addRandom(domainIndependentMutations.random(randomGenerator))

        return Mutator(ms, verbose)
    }
}

