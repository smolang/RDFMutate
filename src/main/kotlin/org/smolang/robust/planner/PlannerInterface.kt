package org.smolang.robust.planner

import org.smolang.robust.planner.pddl.PddlDomain
import org.smolang.robust.planner.pddl.PddlPlan
import org.smolang.robust.planner.pddl.PddlProblem
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit


// interface to external planner
// folder: directory where to find "./runPlanner.sh"
class PlannerInterface(
    val verbose: Boolean,
    private val folder : String = "planner",) {

    private val domainName = "kgDomain"
    private val problemName = "kgProblem"
    private val plan = "kgPlan"

    private val plannerLog = "planerLog"

    // refer to files with relative paths
    private val plannerFile = File("$folder/runPlanner.sh")
    private val domainFile = File("$folder/$domainName.pddl")
    private val problemFile = File("$folder/$problemName.pddl")
    private val planFile = File("$folder/$plan.txt")

    private val logFile = File("$folder/$plannerLog.log")


    // name of domain and problem; timeout in s
    // default timeout: infinite --> no timeout
    fun getPlan (domain: PddlDomain, problem: PddlProblem, timeout : Long = Long.MAX_VALUE) : PddlPlan? {

        if (!plannerFile.exists()) {
            println("ERROR: can not find planner ${plannerFile.absolutePath}.")
            return null
        }

        // TODO: more safe guards, e.g. check if files already exists
        domainFile.writeText(domain.generate(domainName))
        problemFile.writeText(problem.generate(domainName, problemName))


        //val command = "$planner --plan-file $folder/$plan $folder/$domainName.pddl $folder/$problemName.pddl --search sym-bd()"
        val command = "bash ${plannerFile.absolutePath} ${domainFile.absolutePath} ${problemFile.absolutePath} ${planFile.absolutePath}"

        val plannerProcess = Runtime.getRuntime().exec(command)

        // stream to read planner output
        val plannerOutput = BufferedReader(
            InputStreamReader(
                plannerProcess.inputStream
            )
        )

        // if the timeout is infinite --> run with no timeout
        if (timeout == Long.MAX_VALUE)
            plannerProcess.waitFor()
        else {
            val finished = plannerProcess.waitFor(timeout, TimeUnit.SECONDS)
            if (!finished) {
                if (verbose) println("Planner has run out of time --> no plan generated.")
                writeplannerOutputToFile(plannerOutput)
                return null
            }
        }

        writeplannerOutputToFile(plannerOutput)

        // import plan
        val actions =  planFile.readLines().toList().filterNot { it.startsWith(";") }
        return PddlPlan(actions, verbose)
    }

    private fun writeplannerOutputToFile(plannerOutput : BufferedReader) {
        // build string with all planner output
        val sb: StringBuilder = StringBuilder()
        var s: String?
        while ((plannerOutput.readLine().also { s = it }) != null) {
            sb.appendLine(s)
        }

        // save output of planner to file
        logFile.writeText(sb.toString())
    }
}