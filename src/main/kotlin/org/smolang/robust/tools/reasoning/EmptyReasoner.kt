package org.smolang.robust.tools.reasoning

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory


// empty reasoner that is always consistent and does not contain / infer anything
class EmptyReasoner() : MaskReasoner(ModelFactory.createDefaultModel()){
    override fun isConsistent() = true
    fun entailsAll(jenaModel: Model) = jenaModel.isEmpty
    fun containsAll(jenaModel: Model) = jenaModel.isEmpty
}

