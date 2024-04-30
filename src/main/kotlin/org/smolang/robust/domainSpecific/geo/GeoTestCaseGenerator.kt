package org.smolang.robust.domainSpecific.geo

import org.smolang.robust.mutant.*
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.randomGenerator

class GeoTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {

    private val geoOntoPath = "org/smolang/robust/sut/geo/total_mini.ttl"
    fun generateGeoMutants(contractFile : String, shapes: Shapes?) {
        val seed = RDFDataMgr.loadDataset(geoOntoPath).defaultModel
        //for (s in seed.listStatements())
        //    println(s)

        // new contract
        val contract = MutantMask(verbose, shapes, RDFDataMgr.loadDataset(contractFile).defaultModel, useReasonerContainment=true)

        val geoGenerator = GeoMutatorFactory(verbose, 1)

        val mutationNumbers = listOf(2)//,2,3,4,5,6,7,8,9,10)
        for (i in mutationNumbers) {
            val geoMutator = GeoMutatorFactory(verbose, i)
            super.generateMutants(
                seed,
                contract,
                geoMutator,
                100 //10
            )
        }

        saveMutants("org/smolang/robust/sut/geo/mutatedOnt", "thirdTest")
        super.writeToCSV("org/smolang/robust/sut/geo/mutatedOnt/thirdTest.csv")
    }
}

class GeoMutatorFactory(verbose: Boolean, private val NumberMutations: Int): MutatorFactory(verbose) {

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

