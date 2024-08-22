package org.smolang.robust.planner

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.smolang.robust.mutant.MutationSequence
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.mutant.TestCaseGenerator
import org.smolang.robust.planner.pddl.PddlAssertion
import org.smolang.robust.planner.pddl.PddlConstructor
import org.smolang.robust.planner.pddl.PddlDomain
import org.smolang.robust.planner.pddl.PddlProblem

class PlanerBasedTestCaseGenerator(
    val verbose: Boolean,
    val plannerFolder : String = "planner"
) : TestCaseGenerator(verbose) {
    private val mapToPddl = KgPddlMap()

    private val mf = ModelFactory.createDefaultModel()!!

    private val mutationConfigs : List<ActionMutationConfiguration> = listOf(
        // config for testing; switches "isAt" relation and adds relation with new object
        ActionMutationConfiguration(
            setOf(
                mf.createStatement(
                    mf.createResource("?x"),
                    mf.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#isAt"),
                    mf.createResource("?y")
                )
            ),
            setOf(
                mf.createStatement(
                    mf.createResource("?y"),
                    mf.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#isAt"),
                    mf.createResource("?x")
                ),
                mf.createStatement(
                    mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#newObject"),
                    mf.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#isAt"),
                    mf.createResource("?y")
                )
            ),
            listOf(
                mf.createResource("?y"),
                mf.createResource("?x")
            ),
            mapToPddl
        )
    )


    // returns the number of tries to generate the desired number of mutants
    fun generateMutants(
        seed: Model,
        mask: RobustnessMask,
        countDesired: Int,
        plannerTimeout : Long = Long.MAX_VALUE // timeout to run planer in s; default: no timeout
    ): Int {
        // build map from kg to pddl
        for (s in seed.listStatements()) {
            mapToPddl.putAllElmentsIfAbsent(s)
        }

        // build planning domain and map from pddl action names to configs
        val (domain, actionsToConfigs) = buildDomain(seed, mutationConfigs)

        // define goal
        val goal = listOf(
            mapToPddl.toPddl(mf.createStatement(
            mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#newObject"),
            mf.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#isAt"),
            mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
        ))
        )

        // TODO: transform mask to goal + derived predicates for domain

        // build problem file
        val problem = buildProblem(domain, seed, goal)

        // call planner
        if (verbose) println("Calling  external planner")
        val plan = PlannerInterface(verbose, plannerFolder).getPlan(domain, problem, plannerTimeout)

        // extract mutation sequence from plan, or default to empty mutation sequence
        if (plan == null)
            if (verbose) println("WARNING: no plan generated --> do not mutate seed KG")
        val ms = plan?.toMutationSequence(actionsToConfigs, mapToPddl) ?: MutationSequence(verbose)

        // do mutation according to the plan
        val mutator = Mutator(ms, verbose)
        val mutant = mutator.mutate(seed)


        if (mask.validate(mutant)) {
            // valid mutant found
            if (verbose)  println("found valid mutant")
            mutators.add(mutator)
            mutants.add(mutant)
            mutantFiles.add("?")
        }
        return 0
    }

    // returns a domain and a map that maps pddl action names to the providedconfigurations
    private fun buildDomain(
        seed: Model,
        configs : List<ActionMutationConfiguration>
    ) : Pair<PddlDomain, MutableMap<String, ActionMutationConfiguration>> {
        val domain = PddlDomain()

        // extract predicate declarations from seed and map to pddl names
        val pddlRelations = seed.listStatements().toList().mapNotNull { mapToPddl.toPddl(it.predicate) }.toSet()
        // add declarations for extracted relations to domain
        for (r in pddlRelations)
            domain.addPredicate(PddlConstructor.relationToPredDeclaration(r, 2))


        //map from pddl action names to configs
        val actionsToConfigs : MutableMap<String, ActionMutationConfiguration> = mutableMapOf()
        // add actions from mutation configurations
        var i = 0
        for (c in configs) {
            val tempAction = c.asPddlAction("action$i")
            actionsToConfigs["action$i"] = c
            i += 1
            domain.addAction(tempAction)
        }

        return Pair(domain, actionsToConfigs)
    }

    private fun buildProblem(
        domain: PddlDomain,
        seed: Model,
        goal: List<PddlAssertion?>) : PddlProblem {
        val problem = PddlProblem()
        // import objects, i.e. constants from domain
        domain.usedObjects.forEach { problem.addObject(it, "object") }

        for (s in seed.listStatements()) {
            val initAssertion = mapToPddl.toPddl(s)
            if (initAssertion != null)
                problem.addToInitialState(initAssertion)
        }

        for (g in goal)
            if (g != null)
                problem.addToGoalState(g)

        return problem
    }
}