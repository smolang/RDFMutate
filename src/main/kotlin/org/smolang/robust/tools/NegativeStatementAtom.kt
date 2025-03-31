package org.smolang.robust.tools

import org.apache.jena.rdf.model.Statement
import org.smolang.robust.toLocalString

class NegativeStatementAtom(statement: Statement) : StatementAtom(statement) {
    override fun toLocalString(): String {
        return "not${statement.toLocalString()}"
    }
}