package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.Statement
import org.smolang.robust.tools.toLocalString

class PositiveStatementAtom(statement: Statement) : StatementAtom(statement) {
    override fun toLocalString(): String {
        return statement.toLocalString()
    }
}