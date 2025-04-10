package org.smolang.robust.tools
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.mainLogger

class NodeMap {
    private val mapping: MutableMap<Resource, RDFNode> = mutableMapOf()

    // access method to get value
    operator fun get(r : Resource) : RDFNode? {
        return mapping[r]
    }

    // access method to set value
    operator fun set(r: Resource, n : RDFNode) {
        mapping[r] = n
    }


    fun apply(s : Statement, model: Model) : Statement? {
        val sub = apply(s.subject)
        if (!sub.isResource)
            return null

        val pred = apply(s.predicate)
        if (!pred.isResource)
            return null


        return model.createStatement(
            sub.asResource(),
            model.getProperty(pred.toString()),
            apply(s.`object`)
        )
    }

    fun apply(n : RDFNode) : RDFNode {
        if (!n.isResource)
            return n
        // return value, if it is in map, otherwise use provided value
        val r = n.asResource()
        return mapping[r] ?: r
    }


}