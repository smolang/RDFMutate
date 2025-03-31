package org.smolang.robust.tools

import org.apache.jena.rdf.model.Statement
import org.smolang.robust.toLocalString

class PositiveStatementAtom(statement: Statement) : StatementAtom(statement) {
    override fun toLocalString(): String {
        return statement.toLocalString()
    }
}