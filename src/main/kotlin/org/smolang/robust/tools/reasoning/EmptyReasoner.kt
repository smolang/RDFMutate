package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory


// empty reasoner that is always consistent and does not contain / infer anything
class EmptyReasoner() : MaskReasoner(){
    override fun isConsistent() = ConsistencyResult.CONSISTENT
}

