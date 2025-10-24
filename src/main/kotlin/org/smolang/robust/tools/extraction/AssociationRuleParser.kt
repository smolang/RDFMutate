package org.smolang.robust.tools.extraction

import org.apache.jena.rdf.model.Resource
import org.smolang.robust.mutant.AbstractMutation
import org.smolang.robust.mutant.RuleMutation
import org.smolang.robust.mutant.RuleMutationConfiguration
import org.smolang.robust.tools.MutationFileParser
import org.smolang.robust.tools.ruleMutations.FreshNodeAtom
import org.smolang.robust.tools.ruleMutations.MutationAtom
import org.smolang.robust.tools.ruleMutations.NegativeStatementAtom
import org.smolang.robust.tools.ruleMutations.PositiveStatementAtom
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