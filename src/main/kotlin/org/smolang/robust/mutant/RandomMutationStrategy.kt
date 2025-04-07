package org.smolang.robust.mutant

import kotlin.random.Random

// a strategy that can always generate a next mutation sequence by selecting random mutations
class RandomMutationStrategy(
    private val mutationOperators : List<AbstractMutation>,
    private val numberMutations : Int,
    private val selectionSeed : Int = 42) : MutationStrategy() {

    private val generator = Random(selectionSeed)

    override fun hasNextMutationSequence(): Boolean {
        return mutationOperators.any()
    }

    override fun getNextMutationSequence(): MutationSequence {
        assert(mutationOperators.any())
        val ms = MutationSequence()
        for (j in 1..(numberMutations)) {
            val mutation = mutationOperators.random(generator)
            ms.addAbstractMutation(mutation)
        }
        return  ms
    }
}