package mutant

import randomGenerator
import kotlin.reflect.KClass


class MutationSequence(private  val verbose: Boolean) {
    private val mutations : MutableList<AbstractMutation> = mutableListOf()

    val relevantPrefixes: MutableSet<String> = hashSetOf()
    fun addRelevantPrefix(prefix: String) {
        relevantPrefixes.add(prefix)
    }

    // adds a random mutation from the provided list of mutations
    fun addRandom(mutOps: List<KClass<out Mutation>>) {
        val am = AbstractMutation(mutOps.random(randomGenerator), verbose)
        for (p in relevantPrefixes)
            am.addRelevantPrefix(p)
        mutations.add(am)
    }

    fun addRandom(mutOp: KClass<out Mutation>) {
        addRandom(listOf(mutOp))
    }

    fun addWithConfig(mutOp: KClass<out Mutation>, config: MutationConfiguration) {
        val am = AbstractMutation(mutOp, config, verbose)
        for (p in relevantPrefixes)
            am.addRelevantPrefix(p)
        mutations.add(am)
    }

    operator fun get(index: Int) : AbstractMutation {
        return mutations[index]
    }

    fun size() : Int {
        return mutations.size
    }

    // shuffles the element in the sequence
    fun shuffle() {
        mutations.shuffle(randomGenerator)
    }
}