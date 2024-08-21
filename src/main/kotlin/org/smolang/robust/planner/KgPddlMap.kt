package org.smolang.robust.planner

import io.michaelrocks.bimap.HashBiMap
import io.michaelrocks.bimap.MutableBiMap
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.Statement
import org.smolang.robust.planner.pddl.PddlAssertion

// maps resources from the KG to Strings in PDDL and the other way around
class KgPddlMap {
    val kgToPddl : MutableBiMap<RDFNode, String> = HashBiMap()


    fun toPddl(r : RDFNode) : String? {
        return kgToPddl[r]
    }


    fun toKg(s : String) : RDFNode? {
        return kgToPddl.inverse[s]
    }

    fun addMapping(r : RDFNode, s : String) {
        kgToPddl[r] = s
    }

    fun addVariable(v : RDFNode) {
        // add leading
        val varName = if (v.toString().startsWith("?"))
            v.toString()
        else
            "?$v"

        kgToPddl[v] =  varName

    }

    fun putIfAbsent(r : RDFNode) {
        if (!kgToPddl.containsKey(r)) {
            val name: String = if (r.isResource)
                r.asResource().localName
            else if (r.isLiteral)
                r.asLiteral().string.toString()
            else "_"    // we assign just a number for the resource


            var i = 0
            var value = name
            while (kgToPddl.containsValue(name)) {
                // iterate over adding numbers to end of name, if it already exists
                value = "$name-$i"
                i += 1
            }

            addMapping(r, value)
        }
    }

    fun putAllElmentsIfAbsent(s : Statement) {
        putIfAbsent(s.predicate)
        putIfAbsent(s.subject)
        putIfAbsent(s.`object`)
    }

    fun toPddl(s : Statement) : PddlAssertion? {
        val relation = toPddl(s.predicate)
        val argument1 = toPddl(s.subject)
        val argument2 = toPddl(s.`object`)

        // return relation if everything can be mapped
        if (relation != null && argument1 != null && argument2 != null)
            return PddlAssertion(relation,  listOf(argument1, argument2))

        return null
    }

    override fun toString(): String {
        return kgToPddl.keys.map { k -> "$k->${kgToPddl[k]}" }.toString()
    }
}