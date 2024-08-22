package org.smolang.robust.planner

import org.smolang.robust.planner.pddl.PddlDomain
import org.smolang.robust.planner.pddl.PddlPlan
import org.smolang.robust.planner.pddl.PddlProblem
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit


// interface to external planner
class PlannerInterface(val verbose: Boolean) {

    val planner = "python3 ../../planningTools/symk-master/fast-downward.py"

    val folder = "planner"
    val domainName = "kgDomain"
    val problemName = "kgProblem"
    val plan = "kgPlan"

    val plannerLog = "planerLog"

    // refer to files with relative paths
    val domainFile = File("$folder/$domainName.pddl")
    val problemFile = File("$folder/$problemName.pddl")
    val planFile = File("$folder/$plan.txt")

    val logFile = File("$folder/$plannerLog.log")


    // name of domain and problem; timeout in s
    // default timeout: infinite --> no timeout
    fun getPlan (domain: PddlDomain, problem: PddlProblem, timeout : Long = Long.MAX_VALUE) : PddlPlan {
        // TODO: more safe guards, e.g. check if files already exists
        domainFile.writeText(domain.generate(domainName))
        problemFile.writeText(problem.generate(domainName, problemName))



        //val command = "$planner --plan-file $folder/$plan $folder/$domainName.pddl $folder/$problemName.pddl --search sym-bd()"
        val command = "bash $folder/runPlanner.sh ${domainFile.absolutePath} ${problemFile.absolutePath} ${planFile.absolutePath}"

        val plannerProcess = Runtime.getRuntime().exec(command)

        // stream to read planner output
        val plannerOutput = BufferedReader(
            InputStreamReader(
                plannerProcess.inputStream
            )
        )


        //println(command)

        // if the timeout is infinite --> run with no timeout
        if (timeout == Long.MAX_VALUE)
            plannerProcess.waitFor()
        else
            plannerProcess.waitFor(timeout, TimeUnit.SECONDS)


        // build string with all planner output
        val sb: StringBuilder = StringBuilder()
        var s: String?
        while ((plannerOutput.readLine().also { s = it }) != null) {
            sb.appendLine(s)
        }

        // save output of planner to file
        logFile.writeText(sb.toString())

        // import plan
        val actions =  planFile.readLines().toList().filterNot { it.startsWith(";") }
        return PddlPlan(actions, verbose)
    }
}