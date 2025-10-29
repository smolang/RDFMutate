package org.smolang.robust.tools.extraction

import org.smolang.robust.mainLogger
import java.io.File
import java.util.concurrent.TimeUnit

// connects the main project to the subproject written in Scala
class ExtractorBridge(
    val minRuleMatch: Int,   // how often rule matches completely
    val minHeadMatch: Int,   // how often head matches
    val minConfidence: Double,    // ratio how often rule matches when body matches
    val maxRuleLength: Int,   // maximal length of rule (head + body)
    val jarLocation: String = "rules/build/libs/rules-1.0-all.jar"   // location of the JAR to extract operators
) {
    var status = ExtractorStatus.WAITING

    // runs command in extra process
    // timeout in s
    fun String.runCommand(
        workingDir: File,
        timeout: Long): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                //.redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(timeout, TimeUnit.SECONDS)
            mainLogger.info("Command finished with exit value ${proc.exitValue()}")

            val errorMessage =proc.errorStream.bufferedReader().readText()
            val output = proc.inputStream.bufferedReader().readText()

            if (proc.exitValue() == 2 || output.contains("ERROR: invalid arguments")) {
                status == ExtractorStatus.WRONG_ARGUMENTS
                return null
            }
            if (errorMessage.isNotBlank()) {
                status == ExtractorStatus.ERROR
                mainLogger.error("Command produced following error: $errorMessage")
                return null
            }

            status = ExtractorStatus.SUCCESS
            return output
        } catch (_: InterruptedException) {
            status = ExtractorStatus.TIMEOUT
            //e.printStackTrace()
            return null
        }
    }

    fun extractRules(
        ontologyFiles: Set<File>,   // set of file to learn from
    ) : Set<String>? {
        if (ontologyFiles.isEmpty()) {
            mainLogger.warn("No knowledge graphs provided to learn operators from. Empty set of operators is created.")
            return setOf()
        }
        mainLogger.info("Calling sub-module to extract rules from following knowledge graph files: ${ontologyFiles.joinToString { "," }}")
        // command to call JAR of sub-project
        val command = "java -cp $jarLocation org.smolang.robust.patterns.StartClass " +
                "$minRuleMatch $minHeadMatch $minConfidence $maxRuleLength ${ontologyFiles.joinToString(" ")}"

        val workingDir = File("./").absoluteFile
        mainLogger.info("running command: $command")
        val result = command.runCommand(workingDir, 60)

        if (status != ExtractorStatus.SUCCESS) {
            mainLogger.warn("Could not extract rules from knowledge graphs. Status of extraction is $status")
            return null
        }

        // extraction had success
        assert(result != null)
        mainLogger.info("Extraction of rules was successfull")
        return findRulesInResult(result!!)
    }

    // parses raw result and finds contained rules
    fun findRulesInResult(result: String): Set<String> {
        val rules = mutableSetOf<String>()
        var isRule = false // bool to parse string; is true if next line is rule
        result.lines().forEach { line ->
            if (line.contains("RULES END"))
                isRule = false
            else if (line.contains("RULES START"))
                isRule = true
            else if (isRule)
                rules.add(line)
        }
        return rules
    }

    // attempt to load classes dynamically --> failed
    /*fun reflectionApproach() {
        val rulesJarPath = "rules/build/libs/rules-1.0.10-all.jar"
        val rulesJarUrl = java.io.File(rulesJarPath).toURI().toURL()
        val rulesClassLoader = URLClassLoader(arrayOf(rulesJarUrl), null) // Parent classloader is null to avoid leakage
        val patternExtractorClass = rulesClassLoader.loadClass("org.smolang.robust.patterns.PatternExtractor").kotlin

        val extractorJava = patternExtractorClass.java
        val extractorConstructor = extractorJava.constructors.first()
        val extractRulesMethodSet = extractorJava.getMethod("extractRules", Set::class.java)
        val extractRulesMethodFile = extractorJava.getMethod("extractRules", File::class.java)


        // pattern extractor for ORE ontologies
        //val orePatternExtractor = patternExtractorClass.primaryConstructor?.call(
        val orePatternExtractor = extractorConstructor.newInstance(
            50,
            20,
            0.8,
            3
        )
        val associationRuleExtractor = AssociationRuleExtractor()
        val outputELFile = File("tempRules.txt")
        // get all files from folder

        println("Kotlin OntSpecification loaded from: ${OntSpecification::class.java.protectionDomain?.codeSource?.location}")
        val ontologyFile = File("src/test/resources/ruleExtraction/ore_ont_155.owl")
        extractRulesMethodFile.invoke(orePatternExtractor, ontologyFile)
    }
     */


}

enum class ExtractorStatus {
    WAITING,
    SUCCESS,
    TIMEOUT,
    WRONG_ARGUMENTS,
    ERROR
}