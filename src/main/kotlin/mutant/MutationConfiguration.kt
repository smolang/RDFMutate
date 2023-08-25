package mutant

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement


abstract class MutationConfiguration

class SingleStatementConfiguration(private val statement: Statement) : MutationConfiguration() {

    fun getStatement() : Statement {
        return statement
    }
}

open class SingleResourceConfiguration(private val resource: Resource) : MutationConfiguration() {

    fun getResource() : Resource {
        return resource
    }
}

