package org.smolang.robust.tools.extraction

import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.tools.MutationFileParser
import java.io.File
import kotlin.sequences.flatMap

// parses association rules to create abstract mutation operators
class AssociationRuleParser(val rulesFile: File): MutationFileParser() {

    // loads all rules form one file and creates abstract mutations for them
    override fun getAllAbstractMutations(): List<AbstractMutation> {
        val mutationOperators: List<AbstractMutation> = rulesFile.useLines { lines ->
            lines.flatMap { line ->
                //println("parse line \"$line\"")
                AssociationRuleFactory().getAssociationRule(line).getAbstractMutations()
            }.toList()
        }
        return mutationOperators
    }
}