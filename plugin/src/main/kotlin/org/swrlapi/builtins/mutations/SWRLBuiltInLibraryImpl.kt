package org.swrlapi.builtins.mutations

import org.swrlapi.builtins.AbstractSWRLBuiltInLibrary
import org.swrlapi.builtins.arguments.SWRLBuiltInArgument


class SWRLBuiltInLibraryImpl() : AbstractSWRLBuiltInLibrary(
    PREFIX, NAMESPACE, BUILT_IN_NAMES
) {
    override fun reset() {  }

    companion object {
        const val PREFIX = "mutations"
        const val NAMESPACE = "http://swrl.stanford.edu/ontologies/built-ins/5.2.0/mutations.owl#"
        val BUILT_IN_NAMES = setOf("tempBuiltIn")
    }

    fun tempBuiltIn(arguments: List<SWRLBuiltInArgument>): Boolean  {
        return true // always true
    }

}

 

