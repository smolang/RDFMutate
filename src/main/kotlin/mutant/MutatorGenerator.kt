package mutant

open class MutatorGenerator(val verbose: Boolean) {

    open fun randomMutator() : Mutator {
        return Mutator(MutationSequence(verbose), verbose)
    }
}