package org.smolang.robust.planner

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.smolang.robust.mutant.Mutator
import org.smolang.robust.mutant.RobustnessMask
import org.smolang.robust.mutant.TestCaseGenerator
import org.smolang.robust.planner.pddl.PddlDomain
import org.smolang.robust.planner.pddl.PddlProblem

class PlanerBasedTestCaseGenerator(val verbose: Boolean) : TestCaseGenerator(verbose) {
    val mapToPddl = KgPddlMap()

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
        countDesired: Int
    ): Int {
        // build map from kg to pddl
        for (s in seed.listStatements()) {
            mapToPddl.putAllElmentsIfAbsent(s)

        }

        // build planning domain
        val domain = PddlDomain()

        //map from pddl action names to configs
        val actionsToConfigs : MutableMap<String, ActionMutationConfiguration> = mutableMapOf()
        // add actions from mutations
        var i = 0
        for (c in mutationConfigs) {
            val tempAction = c.asPddlAction("action$i")
            actionsToConfigs["action$i"] = c
            i += 1
            domain.addAction(tempAction)
        }

        val problem = PddlProblem()
        // import objects, i.e. constants from domain
        domain.usedObjects.forEach { problem.addObject(it, "object") }

        for (s in seed.listStatements()) {
            val initAssertion = mapToPddl.toPddl(s)
            if (initAssertion != null)
                problem.addToInitialState(initAssertion)
        }

        // TODO: transform mask to goal + derived predicates for domain

        val goal = mapToPddl.toPddl(mf.createStatement(
                mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#newObject"),
                mf.createProperty("http://www.ifi.uio.no/tobiajoh/miniPipes#isAt"),
                mf.createResource("http://www.ifi.uio.no/tobiajoh/miniPipes#segment1")
            ))

        if (goal != null)
            problem.addToGoalState(goal)


        // call planner
        if (verbose) println("Calling  external planner")
        val plan = PlannerInterface(verbose).getPlan(domain, problem)

        val ms = plan.toMutationSequence(actionsToConfigs, mapToPddl)


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
}