package org.smolang.robust.tools

import org.apache.jena.rdf.model.Statement

class NegativeStatementAtom(statement: Statement) : StatementAtom(statement) {
    override fun toLocalString(): String {
        return "not${statement.toLocalString()}"
    }
}