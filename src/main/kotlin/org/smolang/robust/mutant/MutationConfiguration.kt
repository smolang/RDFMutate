package org.smolang.robust.mutant

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.tools.ruleMutations.MutationAtom


abstract class MutationConfiguration {
    override fun toString(): String {
        val className = javaClass.toString().removePrefix("class org.smolang.robust.mutant.")
        return className
    }

    fun removePrefix(s : String): String {
        val delimiters = setOf('#', '/')
        val pos = s.indexOfLast { delimiters.contains(it) }
        //println("$s, $pos")
        return if (pos >= 0)
            s.removeRange(0, pos+1)
        else
            s
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
        return "$className(${resource.localName})"
    }

    fun getResource() : Resource {
        return resource
    }
}

class StringAndResourceConfiguration(private val string: String, r: Resource) : SingleResourceConfiguration(r){
    override fun toString(): String {
        val className = super.toString()
        return "$className($string,${getResource().localName})"
    }

    fun getString() : String {
        return string
    }
}

class DoubleStringAndStatementConfiguration(private val nodeOld: String,
                                            private val nodeNew: String,
                                            private val r: Statement) : MutationConfiguration() {
    override fun toString(): String = "(" +
                "${removePrefix(nodeOld)}," +
                "${removePrefix(nodeNew)}," +
                "$r)"


    fun getOldNode() : String {
        return nodeOld
    }

    fun getNewNode() : String {
        return nodeNew
    }

    fun getStatement() : Statement {
        return r
    }
}

class RuleMutationConfiguration(val body : List<MutationAtom> = listOf(),
                                val head : List<MutationAtom> = listOf(),
                                val bodyVariables : Set<Resource> = setOf(),
                                val headVariables : Set<Resource> = setOf()
) : MutationConfiguration() {

    val variables : Set<RDFNode> get() = run { bodyVariables.union(headVariables) }

    override fun toString(): String {
        return "RuleMutationConfiguration(" +
                "(${body.map { s -> s.toLocalString() }.joinToString(",")}) -> " +
                "(${head.map { s -> s.toLocalString() }.joinToString(",")}))"
    }


}


