package mutant

import kotlin.reflect.KClass


class MutationSequence(private  val verbose: Boolean) {
    private val mutations : ArrayList<AbstractMutation> = ArrayList()

    // adds a random mutation from the provided list of mutations
    fun addRandom(mutOps: List<KClass<out Mutation>>) {
        val am = AbstractMutation(mutOps.random(), verbose)
        mutations.add(am)
    }


    fun addWithConfig(mutOp: KClass<out Mutation>, config: MutationConfiguration) {
        val am = AbstractMutation(mutOp, config, verbose)
        mutations.add(am)
    }

    operator fun get(index: Int) : AbstractMutation {
        return mutations[index]
    }

    fun size() : Int {
        return mutations.size
    }
}