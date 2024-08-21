package org.smolang.robust.mutant

open class MutatorFactory(val verbose: Boolean) {

    open fun randomMutator() : Mutator {
        return Mutator(MutationSequence(verbose), verbose)
    }
}