package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Statement
import org.smolang.robust.randomGenerator
import kotlin.reflect.KClass


class MutationSequence(private  val verbose: Boolean) {
    private val mutations : MutableList<AbstractMutation> = mutableListOf()

    val mutatableAxioms: MutableSet<Statement> = hashSetOf()
    fun addMutatableAxiom(s: Statement) {
        mutatableAxioms.add(s)
    }

    fun addMutatableAxioms(l: List<Statement>) {
        mutatableAxioms.addAll(l)
    }

    // adds a random mutation from the provided list of mutations
    fun addRandom(mutOps: List<KClass<out Mutation>>) {
        val am = AbstractMutation(mutOps.random(randomGenerator), verbose)
        for (a in mutatableAxioms)
            am.addMutatableAxiom(a)
        mutations.add(am)
    }

    fun addRandom(mutOp: KClass<out Mutation>) {
        addRandom(listOf(mutOp))
    }

    fun addWithConfig(mutOp: KClass<out Mutation>, config: MutationConfiguration) {
        val am = AbstractMutation(mutOp, config, verbose)
        for (a in mutatableAxioms)
            am.addMutatableAxiom(a)
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