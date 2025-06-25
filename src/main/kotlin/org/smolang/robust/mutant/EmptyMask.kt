package org.smolang.robust.mutant

import org.apache.jena.rdf.model.Model

// a mask that marks every generated graph as valid
class EmptyMask(verbose: Boolean) : RobustnessMask(verbose, null) {
    // mask accepts all models
    override fun validate(model: Model): Boolean {
        return true
    }
}