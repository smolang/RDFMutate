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

class StringAndResourceConfiguration(private val string: String, r: Resource) : SingleResourceConfiguration(r){
    fun getString() : String {
        return string
    }
}

class DoubleResourceConfiguration(private val resource1: Resource,
                                  private  val resource2: Resource) : MutationConfiguration(){
    fun getResource1() : Resource {
       return resource1
    }

    fun getResource2() : Resource {
        return  resource2
    }
}
