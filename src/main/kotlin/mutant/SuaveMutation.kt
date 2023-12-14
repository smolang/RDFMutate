package mutant

import org.apache.jena.rdf.model.Model

const val suaveIRI = "http://www.metacontrol.org/suave"
const val tomasysIRI = "http://metacontrol.org/tomasys"
const val delimiter = "#"
val qaeIRI = "http://metacontrol.org/tomasys#hasQAestimation"
val solvesF = "http://metacontrol.org/tomasys#solvesF"
val qaType = "http://metacontrol.org/tomasys#isQAtype"



class AddQAEstimationMutation(model: Model, verbose: Boolean) : AddObjectPropertyMutation(model, verbose) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaeIRI)
            )
        )
    }
}

class RemoveQAEstimationMutation(model: Model, verbose: Boolean) : RemoveObjectPropertyMutation(model, verbose) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaeIRI)
            )
        )
    }
}


// depends on the implementation of "AddObjectPropertyMutation":
// assumes that existing relations are deleted, to keep functional property
class ChangeSolvesFunctionMutation(model: Model, verbose: Boolean) : AddObjectPropertyMutation(model, verbose) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(solvesF)
            )
        )
    }
}

class ChangeQualityAttributTypeMutation(model: Model, verbose: Boolean) : AddObjectPropertyMutation(model, verbose) {
    init {
        super.setConfiguration(
            SingleResourceConfiguration(
                model.createResource(qaType)
            )
        )
    }
}