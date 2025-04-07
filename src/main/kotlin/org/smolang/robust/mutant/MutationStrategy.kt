package org.smolang.robust.mutant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// class to represent mutation strategies
abstract class MutationStrategy() {

    // default value that is extracted by parser, if no seed is provided
    companion object {
        val defaultSeed = 42
    }

    // returns true, if the strategy can provide another mutation sequence
    // returns false, if no further sequences should be tried
    abstract fun hasNextMutationSequence() : Boolean

    // returns the next mutation sequence to try
    abstract fun getNextMutationSequence() : MutationSequence
}

@Serializable
enum class MutationStrategyName {
    @SerialName("random")
    RANDOM
}