package org.smolang.robust.tools.ruleMutations

import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import kotlin.plus

class MutationAtomFactory(val prefixMap: Map<String, String> = mapOf()) {

    // turn string representation of atom into atom based on JENA resources
    fun getAtom(ruleAtom: String, variables: Map<String, Resource>): MutationAtom {
        val elements = ruleAtom.split(" ".toRegex())
        assert(elements.size == 3) {"atoms need to contain exactly three elements, i.e., represent triples"}
        val s = parseNode(elements[0].removePrefix("("), variables)
        val p = parseNode(elements[1], variables)
        val o = parseNode(elements[2].removeSuffix(")"), variables)
        assert(s.isResource)
        assert(p.isResource)
        val statement = ResourceFactory.createStatement(
            s.asResource(), ResourceFactory.createProperty(p.toString()), o
        )
        return PositiveStatementAtom(statement)
    }

    // parses string to resource
    private fun parseNode(s: String, variables: Map<String, Resource>): RDFNode {
        if (variables.containsKey(s))
            return variables[s]!!

        if (s.indexOfFirst { c -> c == ':' } > -1) {
            // handle prefix
            val p = s.substringBefore(':') + ":"
            val rest = s.substringAfter(':')
            if (prefixMap.containsKey(p)) {
                return ResourceFactory.createResource(prefixMap[p] + rest)
            }
            else
                println("WARNING: can not find prefix $p in prefix map")
        }
        return ResourceFactory.createResource(s)
    }
}