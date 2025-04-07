package org.smolang.robust.mutant

import org.smolang.robust.tools.reasoning.ReasoningBackend

// a mask without shapes
// if a reasoner is provided as the backend, it is used for consistency check
class EmptyRobustnessMask(
    backend: ReasoningBackend = ReasoningBackend.NONE
) : RobustnessMask(null, backend) {
}