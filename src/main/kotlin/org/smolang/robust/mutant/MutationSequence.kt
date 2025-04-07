package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Statement
import org.smolang.robust.randomGenerator
import kotlin.reflect.KClass


class MutationSequence() {
    private val mutations : MutableList<AbstractMutation> = mutableListOf()

    private val mutableAxioms: MutableSet<Statement> = hashSetOf()

    fun addMutableAxioms(l: List<Statement>) {
        mutableAxioms.addAll(l)
    }

    // adds a random mutation from the provided list of mutations
    fun addRandom(mutOps: List<KClass<out Mutation>>) {
        val am = AbstractMutation(mutOps.random(randomGenerator))
        addAbstractMutation(am)
    }

    fun addRandom(mutOp: KClass<out Mutation>) {
        addRandom(listOf(mutOp))
    }

    fun addWithConfig(mutOp: KClass<out Mutation>, config: MutationConfiguration) {
        val am = AbstractMutation(mutOp, config)
        addAbstractMutation(am)
    }

    fun addAbstractMutation(am : AbstractMutation) {
        for (a in mutableAxioms)
            am.addMutatableStatement(a)
        mutations.add(am)
    }

    // adds all those mutations to be used in mutation sequence
    fun addAllAbstractMutations(list : List<AbstractMutation>) {
        for (am in list)
            this.addAbstractMutation(am)
    }

    operator fun get(index: Int) : AbstractMutation {
        return mutations[index]
    }

    fun size() : Int {
        return mutations.size
    }

    // shuffles the elements in the sequence
    fun shuffle() {
        mutations.shuffle(randomGenerator)
    }
}