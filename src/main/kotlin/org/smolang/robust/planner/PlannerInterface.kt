package org.smolang.robust.planner

import org.smolang.robust.planner.pddl.PddlDomain
import org.smolang.robust.planner.pddl.PddlPlan
import org.smolang.robust.planner.pddl.PddlProblem
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


// interface to external planner
class PlannerInterface(val verbose: Boolean) {

    val planner = "python3 ../../planningTools/symk-master/fast-downward.py"
    val folder = "planner"
    val domainName = "kgDomain"
    val problemName = "kgProblem"
    val plan = "kgPlan.txt"

    fun getPlan (domain: PddlDomain, problem: PddlProblem) : PddlPlan {
        // TODO: more safe guards, e.g. check if files already exists
        File("$folder/$domainName.pddl").writeText(domain.generate(domainName))
        File("$folder/$problemName.pddl").writeText(problem.generate(domainName, problemName))

        val command = "$planner --plan-file $folder/$plan $folder/$domainName.pddl $folder/$problemName.pddl --search sym-bd()"
        val i = Runtime.getRuntime().exec(command)
        /*
        val reader = BufferedReader(
            InputStreamReader(
                i.getInputStream()
            )
        )
        var s: String
        while ((reader.readLine().also { s = it }) != null) {
            println("Script output: $s")
        }
        */

        //println(command)
        // TODO: introduce timeout
        // TODO: save planner output somewhere
        val x = i.waitFor()
        //println(x)

        var actions =  File("$folder/$plan").readLines().toList().filterNot { it.startsWith(";") }
        return PddlPlan(actions, verbose)
    }
}