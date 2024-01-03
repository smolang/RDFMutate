package mutant

import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement


abstract class MutationConfiguration {
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class mutant.")
        return className
    }
}
class SingleStatementConfiguration(private val statement: Statement) : MutationConfiguration() {
    override fun toString(): String {
        val className = super.toString()
        return "$className($statement)"
    }

    fun getStatement() : Statement {
        return statement
    }
}

open class SingleResourceConfiguration(private val resource: Resource) : MutationConfiguration() {

    override fun toString(): String {
        val className = super.toString()
        return "$className($resource)"
    }

    fun getResource() : Resource {
        return resource
    }
}

class StringAndResourceConfiguration(private val string: String, r: Resource) : SingleResourceConfiguration(r){
    override fun toString(): String {
        val className = super.toString()
        return "$className($string,${getResource()})"
    }

    fun getString() : String {
        return string
    }
}

class DoubleResourceConfiguration(private val resource1: Resource,
                                  private  val resource2: Resource) : MutationConfiguration(){

    override fun toString(): String {
        val className = super.toString()
        return "$className($resource1,$resource2)"
    }
    fun getResource1() : Resource {
       return resource1
    }

    fun getResource2() : Resource {
        return  resource2
    }
}
