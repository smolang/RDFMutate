package org.smolang.robust.domainSpecific.suave

import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.Shapes
import org.smolang.robust.mutant.RobustnessMask
import java.io.File
import java.io.FileOutputStream
import org.smolang.robust.mainLogger

class SuaveEvaluationGraphGenerator() {

    // generates data for the graph presented in ISSRE paper
    fun generateGraph(numberOfMutants : Int, outputFile : File) {
        val nameOfMutants = "temp"
        val saveMutants = false
        val ratioDomainDependent1 = 1.0
        val ratioDomainDependent2 = 0.0
        val useAddQAMutation = false


        // lists to collect number of attempts for domain-independent and -specific operators
        val listAttemptsDI2 : MutableList<Int> = mutableListOf()
        val listAttemptsDS2 : MutableList<Int> = mutableListOf()
        val listAttemptsDI5 : MutableList<Int> = mutableListOf()
        val listAttemptsDS5 : MutableList<Int> = mutableListOf()
        val ids = listOf(0,1,2,3,4,5,6,7)
        for (id in ids) {
            mainLogger.info("create mutants for mask with id $id")
            val maskFile = File("sut/suave/masks/mask$id.ttl")
            val shapesGraph = RDFDataMgr.loadGraph(maskFile.absolutePath)

            val mask = RobustnessMask(Shapes.parse(shapesGraph))


            // generate domain-independent mutants
            val sg = SuaveTestCaseGenerator(false)
            val attemptsDI2 = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations=2,
                ratioDomainDependent1,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDI2.add(attemptsDI2)

            // generate domain-specific mutants
            val attemptsDS2 = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations=2,
                ratioDomainDependent2,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDS2.add(attemptsDS2)

            val attemptsDI5 = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations=5,
                ratioDomainDependent1,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDI5.add(attemptsDI5)

            // generate domain-specific mutants
            val attemptsDS5 = sg.generateSuaveMutants(
                numberOfMutants,
                numberOfMutations=5,
                ratioDomainDependent2,
                useAddQAMutation,
                mask,
                nameOfMutants,
                saveMutants
            )
            listAttemptsDS5.add(attemptsDS5)

        }

        // output results to csv file
        FileOutputStream(outputFile).use { fos ->
            val writer = fos.bufferedWriter()
            writer.write("contract,number mutants," +
                    "attemptsDI2,ratioDI2,attemptsDS2,ratioDS2," +
                    "attemptsDI5,ratioDI5,attemptsDS5,ratioDS5")
            writer.newLine()
            for (id in ids) {
                val attemptsDI2 = listAttemptsDI2[id]
                val ratioDI2 = attemptsDI2.toFloat() / numberOfMutants
                val attemptsDS2 = listAttemptsDS2[id]
                val ratioDS2 = attemptsDS2.toFloat() / numberOfMutants
                val attemptsDI5 = listAttemptsDI5[id]
                val ratioDI5 = attemptsDI5.toFloat() / numberOfMutants
                val attemptsDS5 = listAttemptsDS5[id]
                val ratioDS5 = attemptsDS5.toFloat() / numberOfMutants

                writer.write("$id,$numberOfMutants," +
                        "$attemptsDS2,$ratioDS2,$attemptsDI2,$ratioDI2," +
                        "$attemptsDS5,$ratioDS5,$attemptsDI5,$ratioDI5")
                writer.newLine()
            }
            writer.close()
            mainLogger.info("write data for suave evaluation to File $outputFile")
        }
    }
}