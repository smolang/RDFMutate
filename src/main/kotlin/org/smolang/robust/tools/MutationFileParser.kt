package org.smolang.robust.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.tools.ruleMutations.RuleParser
import java.io.File

// abstract class for parsers of files for mutation operators
abstract class MutationFileParser {
    abstract fun getAllAbstractMutations() : List<AbstractMutation>?
}

// factory to generate parsers
class MutationFileParserFactory(val format: MutationOperatorFormat){
    fun getParser(file: File) : MutationFileParser =
        when(format) {
            MutationOperatorFormat.SWRL -> RuleParser(file)
        }
}

@Serializable
enum class MutationOperatorFormat {
    @SerialName("swrl")
    SWRL
}